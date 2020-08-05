package com.github.astridstar.kafka.statistic.data;

import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

public class ConsumerInfo {
    public int id_;
    public String topic_;
    public int producerId_;
    public String groupId_;

    public ConsumerInfo(int id, String topicName, int producerId, String groupId)
    {
        id_ = id;
        topic_ = topicName;
        producerId_ = producerId;
        groupId_ = groupId;
    }

    public void log() {
        GeneralLogger.getDefaultLogger().info(
            String.format("Consumer[%d] Group ID[%s] TopicName[%s] MonitorIDs[%d]",
                    id_, groupId_, topic_, producerId_));
    }
}
