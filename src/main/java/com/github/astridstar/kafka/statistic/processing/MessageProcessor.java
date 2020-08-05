package com.github.astridstar.kafka.statistic.processing;


import com.github.astridstar.kafka.statistic.data.*;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageProcessor extends Thread implements IDataStore {
	
	final String LOGGER_NAME_PREFIX = "GROUP_";
	
	int m_processorId;
	boolean m_isRunning = false;
	
	BlockingQueue<KafkaMessage> m_incomingQ = new LinkedBlockingQueue<>();
	ConcurrentHashMap<String, StatisticData> m_dataMap = new ConcurrentHashMap<String, StatisticData>();

	StatisticRecorder m_recorder;
	CountDownLatch m_terminateLatch;
	Logger m_logger;
	
	private long m_lastReportTimestamp = -1;
	
	public MessageProcessor(int id, CountDownLatch latch)
	{
		super("MessageProcessor-" + id);
		m_processorId = id;
		m_logger = GeneralLogger.getLogger(LOGGER_NAME_PREFIX + m_processorId);
		m_terminateLatch = latch;
		
		m_recorder = new StatisticRecorder();
		
		m_logger.info("===================== " + getName() + " ready for action =====================");
	}
	
	public void cleanup()
	{
		m_isRunning = false;
		interrupt();
	}
	
	@Override
	public void post(KafkaMessage msg) {
		if(!m_incomingQ.offer ( msg ))
			m_logger.warn("Unable to post message onto incoming Q => " + msg.getString());
		
		//m_logger.warn("Adding message from incomingQ => " + msg.getString());
	}
	
	@Override
	public void run() {
		
		long currentTime;
		
		m_isRunning = true;	
		m_recorder.startCalculating();
		while(m_isRunning) {
			try {
				KafkaMessage msg = m_incomingQ.take();
				
				// There is an existing record => producer add this message before
				// Update is most likely from a consumer
				if(m_dataMap.containsKey(msg.messageId_))
					consumePublishedData(msg);
				else
					consumeUnpublishedData(msg);
				
				currentTime = System.currentTimeMillis();
				if(currentTime - m_lastReportTimestamp > Configurator.getReportingIntervalInMs()) {
					m_recorder.print(m_logger);
					m_lastReportTimestamp = currentTime;
				}
			}
			catch(InterruptedException e) {
				GeneralLogger.getDefaultLogger().warn(getName() + " has been interrupted");
			}
			
		}		
		//m_recorder.stopCalculating();
		
		printOutstandingItems();
		m_recorder.logStatistics(m_logger);
				
		GeneralLogger.getDefaultLogger().warn(getName() + " exiting... ");		
		m_terminateLatch.countDown();
	}
	
	private boolean validateMessageContentAndTopic(KafkaMessage msg, StatisticData data) {
		if(msg.topic_.compareTo(data.topic_) != 0) {
			m_logger.warn(String.format("Message topic [%s] differs from the Statistic topic [%s] and yet the message ID is identical!",
					msg.topic_, data.topic_));
			return false;
		}
		
		if(data.sourceId_ != msg.sourceId_) {
			m_logger.warn(String.format("Message sourceId [%s] differs from the Statistic ID [%s] and yet the message ID is identical!",
					msg.topic_, data.topic_));
			return false;
			
		}
			
		return true;
	}
	
	private void consumePublishedData(KafkaMessage msg) {
		StatisticData data = m_dataMap.get(msg.messageId_);
		
		// Need to valid content and topic????
		if(validateMessageContentAndTopic(msg, data)) {
			// Update the destination and source
			if(KafkaConsumerMessage.class.isAssignableFrom(msg.getClass())) {
				//m_logger.warn("Removing consumed messages");
				KafkaConsumerMessage cMsg = (KafkaConsumerMessage) msg;
				data.destId_ = cMsg.consumerId_;
				data.setRecvTimestamp(cMsg.consumedTimestamp_);	
				data.log(m_logger);
				m_dataMap.remove(msg.messageId_);
				//m_recorder.updateLatencies(data.elapsedTimeInMs_);
				m_recorder.update(msg.calculateSizeInBytes(), data.elapsedTimeInMs_);
			} 
			else {
				data.setRecvTimestamp(-1);
				m_logger.warn("Expecting KafkaConsumerMessage but getting " + msg.getClass().toString() + " instead. Not removing the record from the map");
				m_logger.warn(msg.getString());
			}
		} 
		else
			m_logger.warn("Encounter invalid messages");		
	}
	
	private void consumeUnpublishedData(KafkaMessage msg) {
		if(!isValidSession ( msg )) {
			//m_logger.warn("Discarding invalid session message => " + msg.getString());
			return;
		}

		StatisticData statData;
		if(KafkaConsumerMessage.class.isAssignableFrom(msg.getClass())) {
			KafkaConsumerMessage cMsg = (KafkaConsumerMessage) msg;			
			statData = new StatisticData(cMsg);
			m_logger.warn("[DISCARDING message with unpublished state]" + statData.getString());
		}
		else {
			// First time the message is published.  
			// Create the stats object and add to queue
			statData = new StatisticData(msg, -1, -1);
			//m_recorder.updateCounters(msg.calculateSizeInBytes());
			m_dataMap.put(msg.messageId_, statData); 		
		}		
	}

	private boolean isValidSession(KafkaMessage msg) {
		// Check if message belongs to current session or another session
		return msg.messageId_.startsWith(Configurator.SESSION_ID_MSG_ID_PREFIX);
	}
	
	private void printOutstandingItems() {
		m_logger.info("----- Outstanding items in incomingQ => " + m_incomingQ.size() + " -----");
		for(KafkaMessage msg : m_incomingQ) {
			m_logger.info("Outstanding => " + msg.getString());
		}

		m_logger.info("----- Unconsumed items in Data Map => " + m_dataMap.size() + " -----");
		for(StatisticData data : m_dataMap.values()) {
			m_logger.info("Unconsumed => " + data.getString());
		}
	}

	@Override
	public void post(int srcId, String topic, long receivedTimestamp, byte[] rawData) {
		throw new java.lang.UnsupportedOperationException("Not supported yet."); 		
	}
}
