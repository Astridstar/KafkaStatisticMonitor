package com.github.astridstar.kafka.statistic.processing;

import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.data.KafkaMessage;
import com.github.astridstar.kafka.statistic.data.ProducerInfo;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class DataStoreManager extends Thread implements IDataStore{
	BlockingQueue<KafkaMessage> m_queue = new LinkedBlockingQueue<>();
	
	ConcurrentHashMap<Integer, MessageProcessor> m_processors = new ConcurrentHashMap<>();
	
	boolean m_isRunning = false;
	CountDownLatch m_terminateParentLatch = null;
	CountDownLatch m_terminateChildrenLatch = null;
	
	public DataStoreManager()
	{
		super("DataStoreManagerThread");
		
	}
			
	public void configure(CountDownLatch terminateLatch)
	{
		m_terminateParentLatch = terminateLatch;		
		m_terminateChildrenLatch = new CountDownLatch(Configurator.getNumOfProducers());
		
		// Processor ID = producer ID
		int numProducers = Configurator.getNumOfProducers();
				
		for(int i = 1; i <= numProducers; i++)
		{
			ProducerInfo producerInfo = Configurator.getProducerInformation(i);
			MessageProcessor proc = new MessageProcessor(producerInfo.id_, m_terminateChildrenLatch, (int)producerInfo.maxMessageToPublish_);
			proc.start();
			m_processors.put(producerInfo.id_, proc);
		}
	}
	
	public void cleanup()
	{
		m_isRunning = false;
		interrupt();
		
		for(MessageProcessor processor: m_processors.values()) {
			processor.cleanup();
		}
		
		m_processors.clear();
	}


	@Override
	public void post(KafkaMessage msg) {
		try {
			m_queue.put(msg);
		} catch (InterruptedException e) {
			GeneralLogger.getDefaultLogger().error("Exception caught while posting message", e);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		
		m_isRunning = true;
		
		while(m_isRunning)
		{
			try 
			{
				KafkaMessage msg = m_queue.take(); // Block until something enters the queue
				// process the message
				if (!m_processors.containsKey(msg.sourceId_)) {
					GeneralLogger.getDefaultLogger().warn("Unable to find a processor for msg =>" + msg.getString());
				} else {
					// Found a processor
					MessageProcessor processor = m_processors.get(msg.sourceId_);
					processor.post(msg);
				}
			}
			catch(InterruptedException e) {
				GeneralLogger.getDefaultLogger().warn("DataStoreManager has been interrupted");
			}
		}
		
		try {
			GeneralLogger.getDefaultLogger().info("----- DataStoreManager waiting for MessageProcessors to terminate -----");
			m_terminateChildrenLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GeneralLogger.getDefaultLogger().info("----- Items waiting for dispatch => " + m_queue.size() + " -----");
		for(KafkaMessage msg : m_queue) {
			GeneralLogger.getDefaultLogger().info("To dispatch => " + msg.getString());
		}
		
		GeneralLogger.getDefaultLogger().warn("DataStoreManager thread terminating... ");	
		m_terminateParentLatch.countDown();
	}

	@Override
	public void post(int srcId, String topic, long receivedTimestamp, byte[] rawData) {
		throw new java.lang.UnsupportedOperationException("Not supported yet."); 		
	}

}
