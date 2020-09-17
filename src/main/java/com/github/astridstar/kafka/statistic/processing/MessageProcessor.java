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
	long m_numOfRecordsProcessed = 0;

	BlockingQueue<KafkaMessage> m_incomingQ = new LinkedBlockingQueue<>();
	ConcurrentHashMap<String, StatisticData> m_dataMap = new ConcurrentHashMap<String, StatisticData>();

	Stats m_recorder;
	CountDownLatch m_terminateLatch;
	Logger m_logger;
	
	private long m_lastReportTimestamp = -1;
	private int m_expectedMessageCount;
	
	public MessageProcessor(int id, CountDownLatch latch, int expectedMessageCount)
	{
		super("MessageProcessor-" + id);
		m_processorId = id;
		m_logger = GeneralLogger.getLogger(LOGGER_NAME_PREFIX + m_processorId);
		m_terminateLatch = latch;

		m_expectedMessageCount = expectedMessageCount;
		m_recorder = new Stats(m_expectedMessageCount , Configurator.getReportingIntervalInMs(), m_logger);

		m_logger.info("===================== " + getName() + " ready for action =====================");
	}
	
	public void cleanup()
	{
		m_isRunning = false;
		interrupt();
	}
	
	@Override
	public void post(KafkaMessage msg) {
		m_logger.debug("Adding message to incomingQ => " + msg.getString());

		if(!m_incomingQ.offer ( msg ))
			m_logger.warn("Unable to post message onto incoming Q => " + msg.getString());
	}
	
	@Override
	public void run() {
		
		long currentTime;
		
		m_isRunning = true;	
		//m_recorder.startCalculating();
		while(m_isRunning) {
			try {
				// Q will block until there is something in the Q
				KafkaMessage msg = m_incomingQ.take ( );

				// There is an existing record => producer add this message before
				// Update is most likely from a consumer
				if (m_dataMap.containsKey ( msg.messageId_ ))
					consumePublishedData ( msg );
				else
					consumeUnpublishedData ( msg );

				// Dump the amount of items left on the Q
				if(m_expectedMessageCount == m_numOfRecordsProcessed) {
					m_logger.info ( "[STAT] Reached expected number of messages count => " + m_expectedMessageCount + " (processed " + m_numOfRecordsProcessed + ")");
					m_recorder.printWindow();
					m_recorder.printTotal();
				}

				//m_logger.info ( "----- Outstanding items in incomingQ => " + m_incomingQ.size ( ) + " -----" );
				m_logger.debug ( "[Data Map] => " + m_dataMap.size ( ) + " items " );
			} catch(InterruptedException e) {
				GeneralLogger.getDefaultLogger().warn(getName() + " has been interrupted");
			}
			
		}		

		printOutstandingItems();
		m_recorder.printTotal();
				
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
			m_logger.warn ( String.format ( "Message sourceId [%s] differs from the Statistic ID [%s] and yet the message ID is identical!" ,
					msg.topic_ , data.topic_ ) );
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
				m_logger.debug("Removing consumed messages");

				KafkaConsumerMessage cMsg = (KafkaConsumerMessage) msg;
				data.destId_ = cMsg.consumerId_;
				data.setRecvTimestamp(cMsg.consumedTimestamp_);	
				data.log(m_logger);
				m_dataMap.remove(msg.messageId_);

				m_numOfRecordsProcessed++;
				m_recorder.record ( (int) m_numOfRecordsProcessed, (int) data.elapsedTimeInMs_, (int) msg.calculateSizeInBytes(), cMsg.consumedTimestamp_ );
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
		m_logger.info("[FINAL] Outstanding items in incomingQ => " + m_incomingQ.size() + " -----");
		for(KafkaMessage msg : m_incomingQ) {
			m_logger.info("[FINAL] Outstanding => " + msg.getString());
		}

		m_logger.info("[FINAL] Unconsumed items in Data Map => " + m_dataMap.size() + " -----");
		for(StatisticData data : m_dataMap.values()) {
			m_logger.info("[FINAL] Unconsumed => " + data.getString());
		}
	}

	@Override
	public void post(int srcId, String topic, long receivedTimestamp, byte[] rawData) {
		throw new java.lang.UnsupportedOperationException("Not supported yet."); 		
	}
}
