package com.github.astridstar.kafka.statistic.processing;

import java.text.ParseException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.data.KafkaConsumerMessage;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;


public class Consumer extends Thread {
	
	private static final String DEF_CONSUMER_THRD_PREFIX = "Consumer-";
	private static final String DEF_CONSUMER_LOG_PREFIX = "KFC_";
	private static final int DEF_POLLING_RATE_MS = 1000;
	
	private final int 			m_consumerId;
	private final int 			m_interestedPublisher;
	private final Logger 		m_logger;
	private final String 		m_topic;
	private final String		m_groupId;
	private final IDataStore 	m_datastore;
	private boolean				m_keepRunning = true;

	private KafkaConsumer<String, byte[]> m_consumer ;
	private final CountDownLatch m_terminateLatch;
	
	public Consumer(int id, String groupId, String topic, IDataStore ds, int interestedPublisher, CountDownLatch latch)
	{
		super(DEF_CONSUMER_THRD_PREFIX + id);
		m_consumerId = id;
		m_logger = GeneralLogger.getLogger(DEF_CONSUMER_LOG_PREFIX + m_consumerId);
		m_topic = topic;
		m_datastore = ds;
		m_groupId = groupId;
		m_consumer = null;
		m_interestedPublisher = interestedPublisher;
		m_terminateLatch = latch;
		
		m_logger.info("------------------- " + DEF_CONSUMER_LOG_PREFIX + m_consumerId + " ready -------------------");
	}	
	
	public void configure(Properties consumerProps)
	{
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, m_groupId);
		consumerProps.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, String.valueOf(m_consumerId));

        m_consumer = new KafkaConsumer<>(consumerProps);
		m_consumer.subscribe(Collections.singletonList(m_topic), new ConsumerRebalanceListener ( ) {
			@Override
			public void onPartitionsRevoked(Collection <TopicPartition> partitions) {
				partitions.forEach ( topicPartition -> m_logger.info ( "[PARTITION-REVOKED] " + topicPartition.toString () ) );
			}

			@Override
			public void onPartitionsAssigned(Collection <TopicPartition> partitions) {
				partitions.forEach ( topicPartition -> m_logger.info ( "[PARTITION-ASSIGNED] " + topicPartition.toString () ) );
			}

			@Override
			public void onPartitionsLost(Collection <TopicPartition> partitions) {
				partitions.forEach ( topicPartition -> m_logger.info ( "[PARTITION-LOST] " + topicPartition.toString () ) );
			}
		});
		
		m_logger.info(m_consumer.groupMetadata().toString());
		Set <TopicPartition> partitions = m_consumer.assignment();
		partitions.forEach ( partition -> m_logger.info ( "Partition " + partition.partition() + ", Topic " + partition.topic () ) );
	}
	
	public void cleanup() {
		m_keepRunning = false;				
		m_consumer.wakeup();
	}
	
	@Override
	public void run() {
		m_keepRunning = true;
		while(m_keepRunning) {
			try {
			    ConsumerRecords<String, byte[]> records = m_consumer.poll(Duration.ofMillis(DEF_POLLING_RATE_MS));
			    for (ConsumerRecord<String, byte[]> record : records) {
			    	try {
			    		if(record.value() == null) {
			    			m_logger.error("NO CONSUMER RECORDS!!!!");
			    			continue;
			    		}
			    		
				    	if(Configurator.getBIsForwardingEnabled()) {
							m_logger.info("[RECEIVED] offset = " + record.offset()
									+ ", key = " + record.key() );
				    		m_datastore.post(m_interestedPublisher, record.topic(), record.timestamp(),record.value());
				    	}
				    	else {
					    	KafkaConsumerMessage incomingM = new KafkaConsumerMessage(record.value(), m_consumerId);
							m_logger.info("[RECEIVED] offset = " + record.offset()
									+ ", key = " + record.key()
									+ ", value = " + incomingM.getString());
				    		m_datastore.post(incomingM);
				    	}
					} catch (ParseException e) {
						m_logger.error("Exception caught while parsing the record.", e);
						m_logger.error("Unable to convert record for data store processing => " + Arrays.toString(record.value()));
					}
			    }

			    if(records.count () <=0) continue;
				m_consumer.commitAsync ( (offsets , exception) -> {
					if(exception == null){
						// Commit is successful
						offsets.forEach ( ( (topicPartition , offsetAndMetadata) ->
										m_logger.info ( "[COMMITTED] Partition " + topicPartition.partition ()
										+ ", Topic " + topicPartition.topic ()
										+ ", Offset " + offsetAndMetadata.offset ()
										+ ", Metadata " + offsetAndMetadata.metadata ())) );
					} else {
						//Commit fails
						m_logger.error ( "[COMMIT-FAILED] ", exception );
					}
				} );
			} catch(WakeupException e) {
				GeneralLogger.getDefaultLogger().debug(getName() + " has been waken.");
			}
		}

		m_consumer.unsubscribe();
		m_consumer.close();
		m_logger.info("Consumer closed.");
		GeneralLogger.getDefaultLogger().warn(getName() + " thread terminating ...");
		m_terminateLatch.countDown();
	}
}
