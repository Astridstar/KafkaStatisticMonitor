package com.github.astridstar.kafka.statistic.data;

import java.text.ParseException;

import static com.github.astridstar.kafka.statistic.loggers.GeneralLogger.*;

public class KafkaConsumerMessage extends KafkaMessage {
    public int consumerId_;
    public long consumedTimestamp_;

    public KafkaConsumerMessage()
    {
        super();
        consumerId_ = 0;
        consumedTimestamp_ = 0;
    }

    public KafkaConsumerMessage(String msgId, int srcId, long time, String topic, int destId, long recvTime, byte[] content)
    {
        super(msgId, srcId, time, topic, content);
        consumedTimestamp_ = recvTime;
    }

    public KafkaConsumerMessage(byte[] rawData, int destId) throws ParseException
    {
        super(rawData);
        consumerId_ = destId;
        consumedTimestamp_ = System.currentTimeMillis();
    }

    public KafkaConsumerMessage(KafkaMessage parent, int desId) throws ParseException
    {
        messageId_ = parent.messageId_;
        sourceId_ = parent.sourceId_;
        timestamp_ = parent.timestamp_;
        topic_ = parent.topic_;
        content_ = parent.content_;
        consumerId_ = desId;
        consumedTimestamp_ = System.currentTimeMillis();
    }

    @Override
    public void log()
    {
        getDefaultLogger().info(String.format("[KAFKA_M]|MsgId[%s]|Producer[%d]|Timestamp[%d]|TopicName[%s]|RevTimestamp[%d]|Consumer[%d]|Payload[%d]Bytes",
                messageId_, sourceId_, timestamp_, topic_, consumedTimestamp_, consumerId_, (content_ == null) ? 0 : content_.length));
    }

    @Override
    public String getString()
    {
        return String.format("[KAFKA_M]|MsgId[%s]|Producer[%d]|Timestamp[%d]|TopicName[%s]|RevTimestamp[%d]|Consumer[%d]|Payload[%d]Bytes",
                messageId_, sourceId_, timestamp_, topic_, consumedTimestamp_, consumerId_, (content_ == null) ? 0 : content_.length);
    }
}
