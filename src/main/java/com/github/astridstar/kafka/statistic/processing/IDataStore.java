package com.github.astridstar.kafka.statistic.processing;

import com.github.astridstar.kafka.statistic.data.KafkaMessage;

interface IDataStore {
	void post(KafkaMessage msg);
	void post(int srcId, String topic, long receivedTimestamp, byte[] rawData);
}
