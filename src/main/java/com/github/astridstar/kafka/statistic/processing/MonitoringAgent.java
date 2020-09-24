package com.github.astridstar.kafka.statistic.processing;

import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.data.ConsumerInfo;
import com.github.astridstar.kafka.statistic.data.KafkaMessage;
import com.github.astridstar.kafka.statistic.data.ProducerInfo;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MonitoringAgent implements IDataStore {
	
	DataStoreManager m_datasource = null;
	
	ConcurrentHashMap<Integer, Producer> m_producers = new ConcurrentHashMap<>();
	ConcurrentHashMap<Integer, Consumer> m_consumers = new ConcurrentHashMap<>();
	ConcurrentHashMap<Integer, Forwarder> m_forwarders = new ConcurrentHashMap<>();
	
	CountDownLatch m_terminateLatch = null;
		
	public MonitoringAgent()
	{
	}
	
	public void configure()
	{
		if(Configurator.getBIsForwardingEnabled())
			m_terminateLatch = new CountDownLatch(Configurator.getNumOfProducers() + Configurator.getNumOfConsumers());
		else
			m_terminateLatch = new CountDownLatch(Configurator.getNumOfProducers() + Configurator.getNumOfConsumers() + 1); // DataStoreManager
		
		// Create producers
		int number = Configurator.getNumOfProducers();
		if(number > 0) {
	        Properties producerProps = new Properties();

	        try {
	        	producerProps.load(new FileReader(Configurator.DEFAULT_CONFIG_DIR + File.separator + "producer.properties" ));
	        } catch (IOException e) {
	            e.printStackTrace();
	        }	

	        if(Configurator.getBIsForwardingEnabled()) {
				for(int i = 1; i <= number; i++)
				{
					ProducerInfo info = Configurator.getProducerInformation(i);
					Forwarder proc = new Forwarder(info.id_,info.partitionId_, m_terminateLatch);
					proc.configure(producerProps);
					m_forwarders.put(info.id_, proc);
	
					proc.start();
				}
	        	
	        }
	        else {
	    		m_datasource = new DataStoreManager();
	    		m_datasource.configure(m_terminateLatch);
	    		
				for(int i = 1; i <= number; i++)
				{
					ProducerInfo info = Configurator.getProducerInformation(i);
					Producer proc = new Producer( info.id_,
												  info.topic_,
											  this, info.intervalMessageCount_,
												  info.payloadFile_,
												  info.maxMessageToPublish_,
												  info.partitionId_,
												  m_terminateLatch);
					proc.configure(producerProps);
					m_producers.put(info.id_, proc);
	
					proc.start();
				}
	        }
		}
		
		// Create consumers
		number = Configurator.getNumOfConsumers();
		if(number > 0) {
	        Properties consumerProps = new Properties();

	        try {
	        	consumerProps.load(new FileReader(Configurator.DEFAULT_CONFIG_DIR + File.separator + "consumer.properties" ));
	        } catch (IOException e) {
	            e.printStackTrace();
	        }	
	        
			for(int i = 1; i <= number; i++)
			{
				ConsumerInfo info = Configurator.getConsumerInformation(i);
				Consumer proc = new Consumer(i, info.groupId_, info.topic_, this, info.producerId_, m_terminateLatch);
				proc.configure(consumerProps);
				m_consumers.put(i, proc);
				
				proc.start();
			}
		}		
	}
	
	public void start()
	{
		if(m_datasource == null)
			return;
		
		m_datasource.start();
	}
	
	public void cleanup()
	{
		GeneralLogger.getDefaultLogger().info("MonitoringAgent shutdown initiated ...");

		// clean up the producers and consumers first
		m_producers.forEach((k,v)-> v.cleanup() );

		m_consumers.forEach((k,v)-> v.cleanup() );

		m_forwarders.forEach((k,v)-> v.cleanup() );

		// then clean up the data source
		if(m_datasource != null) m_datasource.cleanup ( );
		
		try {
			GeneralLogger.getDefaultLogger().info("MonitoringAgent waiting for all to shutdown ....");
			m_terminateLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			GeneralLogger.getDefaultLogger().info("MonitoringAgent terminating ....");
		}
	}

	@Override
	public void post(KafkaMessage msg) {
		if(m_datasource == null)
			return;
		m_datasource.post(msg);	
	}

	@Override
	public void post(int srcId, String topic, long receivedTimestamp, byte[] rawData) {
		if (!m_forwarders.containsKey ( srcId )) {
			GeneralLogger.getDefaultLogger().error ("No forwarder can be found for ID=" + srcId);
			return;
		}
		var fwder = m_forwarders.get(srcId);
		if(fwder == null) {
			GeneralLogger.getDefaultLogger().error ("No forwarder can be retrieved for ID=" + srcId);
			return;
		}
		fwder.post(srcId, topic, receivedTimestamp, rawData);
	}
}
