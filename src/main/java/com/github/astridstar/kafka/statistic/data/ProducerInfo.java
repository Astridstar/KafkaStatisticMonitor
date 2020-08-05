package com.github.astridstar.kafka.statistic.data;

import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

public class ProducerInfo {
    public int id_;
    public String topic_;
    public int intervalMessageCount_;
    public String payloadFile_;
    public long maxMessageToPublish_;

    public ProducerInfo(int id, String topicName)
    {
        id_ = id;
        topic_ = topicName;
        intervalMessageCount_ = 1;
        payloadFile_ = "";
        maxMessageToPublish_ = 100;
    }

    public void log()
    {
        GeneralLogger.getDefaultLogger().info(
                String.format("ProducerID[%d] TopicName[%s] IntervalMsgCount[%d] Messages to publish[%d] Payload file[%s]",
                        id_, topic_, intervalMessageCount_, maxMessageToPublish_, payloadFile_));
    }
}
