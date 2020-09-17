package com.github.astridstar.kafka.statistic.processing;


import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.data.KafkaMessage;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Forwarder extends Thread implements IDataStore {
	
	private static final String DEF_PRODUCER_THRD_PREFIX = "Producer-";
	
	private final int 	 	m_producerId;
	private final String 	m_producerIdStr;
	private final Logger	m_logger;
	private final int		m_assignedPartitionId;

	private boolean			m_keepRunning;
	private KafkaProducer<String, byte[]> m_kafkaProducer = null;
	private final CountDownLatch m_terminateLatch;
	BlockingQueue<ForwarderContent> m_incomingQ = new LinkedBlockingQueue <> ( );
	
	public class ForwarderContent{
		private final String topic_;
		private final byte[] rawData_;
		
		public ForwarderContent(String topic, byte[] data) {
			topic_ = topic;
			rawData_ = data;
		}
		public byte[] getRawData() {
			return rawData_;
		}
		public String getTopic() {
			return topic_;
		}
		public String rawDataToString() {
			try {
				
				KafkaMessage msg = new KafkaMessage(rawData_);
				return msg.getString();
				
			} catch (NullPointerException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "Unable to decode raw data";
		}
	}
	
	public Forwarder(int id, int partitionId, CountDownLatch latch)
	{
		super(DEF_PRODUCER_THRD_PREFIX + id);
		m_producerId = id;
		m_producerIdStr = Configurator.DEFAULT_LOGGER_GROUP_PREFIX + m_producerId;
		m_assignedPartitionId = partitionId;
		m_terminateLatch = latch;
		m_logger = GeneralLogger.getLogger(m_producerIdStr);
		m_keepRunning = true;
	}
	
	public void configure(Properties producerProps)
	{
		producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, m_producerIdStr);
    	//producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, Configurator.SESSION_ID_MSG_ID_PREFIX + m_producerIdStr);

    	m_kafkaProducer = new KafkaProducer <> ( producerProps );
	}
	
	public void cleanup() {
		m_keepRunning = false;
		interrupt();
	}
	
	@Override
	public void run() {
		m_keepRunning = true;
		
		while(m_keepRunning) {
			try {
				forward(m_incomingQ.take());
			}
			catch(InterruptedException e) {
				GeneralLogger.getDefaultLogger().warn(getName() + " has been interrupted");
			}
		}		
				
		m_kafkaProducer.close();
		m_logger.info("Forwarder closed now.");
		GeneralLogger.getDefaultLogger().warn(getName() + " thread terminating ...");
		m_terminateLatch.countDown();
	}
	
	private void forward(ForwarderContent content)
	{
		if(content == null || content.getRawData() == null) {
			m_logger.warn("Invalid content received. Abandoning this receive content");
			return;
		}
	
		String decoded = content.rawDataToString();
		// Send asynchronously
		ProducerRecord<String, byte[]> record;
		if(Configurator.getBToPublishWithKey()) {
			if(m_assignedPartitionId >= 0) // Publish to specific partition + specific key
				record = new ProducerRecord <> ( content.getTopic ( ) , m_assignedPartitionId, String.valueOf ( m_producerId ) , content.getRawData ( ) );
			else  // Publish to random partition + specific key
				record = new ProducerRecord <> ( content.getTopic ( ) , String.valueOf ( m_producerId ) , content.getRawData ( ) );
		}
		else if(m_assignedPartitionId >= 0)  // Publish to specific partition without a key
			record = new ProducerRecord<>(content.getTopic(), m_assignedPartitionId, null, content.getRawData());
		else  // Publish to random partition
			record = new ProducerRecord<>(content.getTopic(), content.getRawData());
		
		m_kafkaProducer.send(record, new ProducerCallback(System.currentTimeMillis(), m_producerId, decoded, m_logger));
		m_logger.debug("[FORWARD-REQ]" + decoded);
	}

	@Override
	public void post(KafkaMessage msg) {
		throw new java.lang.UnsupportedOperationException("Not supported yet."); 
	}
	
	@Override
	public void post(int srcId, String topic, long receivedTimestamp, byte[] rawData) {
		if (m_incomingQ.offer ( new ForwarderContent ( topic , rawData ) )) return;
		m_logger.warn("Unable to post raw message from " + srcId + " onto incoming Q for topic(" + topic + ")");
	}

	class ProducerCallback implements Callback {
	
	    private final long startTime;
	    private final int key;
	    private Logger m_logger;
	    private String m_contentStr;
	    
	    public ProducerCallback(long startTime, int key, String content, Logger logger) {
	        this.startTime = startTime;
	        this.key = key;
	        this.m_logger = logger;
	        this.m_contentStr = content;
	    }
	
	    /**
	     * A callback method the user can implement to provide asynchronous handling of request completion. This method will
	     * be called when the record sent to the server has been acknowledged. When exception is not null in the callback,
	     * metadata will contain the special -1 value for all fields except for topicPartition, which will be valid.
	     *
	     * @param metadata  The metadata for the record that was sent (i.e. the partition and offset). Null if an error
	     *                  occurred.
	     * @param exception The exception thrown during processing of this record. Null if no error occurred.
	     */
	    public void onCompletion(RecordMetadata metadata, Exception exception) {
	        long latency = System.currentTimeMillis() - startTime;
	        
	        if (metadata != null) {
	        	m_logger.info(
	                "[RE-PUBLISHED](" + key + "," + m_contentStr + " ) sent to partition(" + metadata.partition() +
	                    "), offset(" + metadata.offset() + ") with latency(" + latency + "ms)");
	        } else {
	            exception.printStackTrace();
	            m_logger.error("[RE-PUBLISHED-FAILED] from source " + key + "," + m_contentStr);
	            m_logger.error(exception.getMessage());
	        }
	    }
	}

}
