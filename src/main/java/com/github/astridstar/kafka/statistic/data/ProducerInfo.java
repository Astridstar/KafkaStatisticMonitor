package com.github.astridstar.kafka.statistic.data;

import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

public class ProducerInfo {
    public int id_;
    public String topic_;
    public int intervalMessageCount_;
    public String payloadFile_;

    public ProducerInfo(int id, String topicName)
    {
        id_ = id;
        topic_ = topicName;
        intervalMessageCount_ = 1;
        payloadFile_ = "";
    }

    public void log()
    {
        GeneralLogger.getDefaultLogger().info(
                String.format("ProducerID[%d] TopicName[%s] IntervalMsgCount[%d] Payload file[%s]",
                        id_, topic_, intervalMessageCount_, payloadFile_));
    }
}
