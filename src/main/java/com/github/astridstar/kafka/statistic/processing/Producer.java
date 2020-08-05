package com.github.astridstar.kafka.statistic.processing;

import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.data.KafkaMessage;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Producer extends Thread {
	
	private static final String DEF_PRODUCER_THRD_PREFIX = "Producer-";

	private int 		m_producerId = -1;
	private String 		m_producerIdStr = "";
	private String 		m_publishedTopic = "";
	private boolean		m_keepRunning = true;
	private IDataStore	m_datastore = null;
	private int 		m_intervalMessageCount = 1;
	private String 		m_payloadFile = "";
	private byte[] 		m_payload = null;
	private long 		m_elapsedTimeInMs = 5000;
	private long		m_startTimeInMs = 0;
	private long		m_maxMessageCount;

	private KafkaProducer<Integer, byte[]> m_kafkaProducer = null;
	private final CountDownLatch m_terminateLatch;
	private final Logger m_logger;

	private long MESSAGE_ID = 0;
	synchronized private String getNextMessageId()
	{
		if(MESSAGE_ID >= Long.MAX_VALUE)
			MESSAGE_ID = 0;

		return String.format("%s%d%s%d", Configurator.SESSION_ID_MSG_ID_PREFIX, m_producerId, Configurator.DEFAULT_MSG_ID_SEPARATOR, ++MESSAGE_ID);
	}

	public Producer(int id, String publishingTopic, MonitoringAgent ds, int intervalMsgCount, String payloadFile, long maxMessageCount, CountDownLatch latch)
	{
		super(DEF_PRODUCER_THRD_PREFIX + id);
		m_producerId = id;
		m_producerIdStr = Configurator.SESSION_ID_MSG_ID_PREFIX + "_" + Configurator.DEFAULT_LOGGER_GROUP_PREFIX + m_producerId;
		m_publishedTopic = publishingTopic;
		m_datastore = ds;
		m_intervalMessageCount = intervalMsgCount;
		m_payloadFile = payloadFile;
		m_terminateLatch = latch;
		m_maxMessageCount = maxMessageCount;
		m_logger = GeneralLogger.getLogger(Configurator.DEFAULT_LOGGER_GROUP_PREFIX + m_producerId);
	}
	
	public void configure(Properties producerProps)
	{
		producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, m_producerIdStr);
		if(Configurator.getBIsTransactionsEnabled())
			producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, Configurator.SESSION_ID_MSG_ID_PREFIX + m_producerIdStr);

        // Read
		if(m_payloadFile.length() > 0) {
	        try {
	        	m_payload = Files.readAllBytes(Paths.get(m_payloadFile));	            
	        } catch (IOException e) {
				m_logger.warn("IO error while parsing payload file (payload file enabled)", e);
	        }		
		}
		m_elapsedTimeInMs = Configurator.getProducersRestDurationInMs();
		m_kafkaProducer = new KafkaProducer<>(producerProps);
	}
	
	public void cleanup() {
		m_keepRunning = false;
		interrupt();
	}
	
	@Override
	public void run() {
		long currentTime = 0;
		long messageCount = 0;
		m_keepRunning = true;
		
		if(Configurator.getBIsTransactionsEnabled())
			m_kafkaProducer.initTransactions();
		
		m_startTimeInMs = System.currentTimeMillis();
		while(m_keepRunning) {
			currentTime = System.currentTimeMillis();
			try {
				messageCount = publish(messageCount);

				long timeDiff = currentTime - m_startTimeInMs;
				if(timeDiff < m_elapsedTimeInMs) Thread.sleep ( m_elapsedTimeInMs - timeDiff );
				else Thread.sleep ( m_elapsedTimeInMs );

			} catch (InterruptedException e) {
				GeneralLogger.getDefaultLogger().warn(getName() + " has been interrupted.");
			}

			// Published required number of messages, time to close the producer
			if(m_maxMessageCount <= messageCount) break;

			//if((currentTime - m_startTimeInMs)/1000 >= Configurator.getPublishingDurationInSec() )
			//	break;
		}
		
		m_kafkaProducer.close();
		GeneralLogger.getDefaultLogger().warn(getName() + " thread terminating ... after publishing " + messageCount + " messages.  Target=" + m_maxMessageCount);
		m_terminateLatch.countDown();
	}
	
	private long publish(long currentMessageCount)
	{
		if(m_intervalMessageCount <= 0 || currentMessageCount >= m_maxMessageCount)
			return currentMessageCount;
		
		if(Configurator.getBIsTransactionsEnabled())
			m_kafkaProducer.beginTransaction();

		int counter = 0;
		for(int i = 0; i < m_intervalMessageCount; i++) {
			// Construct message
			String mId = getNextMessageId();
			KafkaMessage message = new KafkaMessage(mId, m_producerId, System.currentTimeMillis(), m_publishedTopic, m_payload);

			// Send asynchronously
			ProducerRecord<Integer, byte[]> record;
			if(Configurator.getBToPublishWithKey())
				record = new ProducerRecord<>(m_publishedTopic, m_producerId, message.serialize());
			else
				record = new ProducerRecord<>(m_publishedTopic, message.serialize());

			try {
				m_kafkaProducer.send ( record ,
						new ProducerCallbackImpl ( System.currentTimeMillis ( ) , message ) );
				m_datastore.post ( message );

				counter++;
			} catch	(Exception e) {
				m_logger.warn ( "Exception caught trying to publish messages", e );
				break;
			}

			if(counter >= m_maxMessageCount) break;
		}
		
		if(Configurator.getBIsTransactionsEnabled())
			m_kafkaProducer.commitTransaction();
		
		m_kafkaProducer.flush();
		return counter + currentMessageCount;
	}
}

