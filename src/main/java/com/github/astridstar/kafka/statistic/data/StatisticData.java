package com.github.astridstar.kafka.statistic.data;

import org.slf4j.Logger;

public class StatisticData extends KafkaMessage {

    public long recvTimestamp_;
    public long destId_;
    public long elapsedTimeInMs_;

    public StatisticData(String msgId, int srcId, long srcTime, String topic, long recvTime, long destId, byte[] content) {
        super(msgId, srcId, srcTime, topic, content);
        recvTimestamp_ = recvTime;
        destId_ = destId;
        elapsedTimeInMs_ = calculateElapsedTimeInMs();
    }

    public StatisticData(KafkaMessage msg, long recvTime, long destId) {
        super(msg.messageId_, msg.sourceId_, msg.timestamp_, msg.topic_, msg.content_);
        recvTimestamp_ = recvTime;
        destId_ = destId;
        elapsedTimeInMs_ = calculateElapsedTimeInMs();
    }

    public StatisticData(KafkaConsumerMessage msg) {
        super(msg.messageId_, msg.sourceId_, msg.timestamp_, msg.topic_, msg.content_);
        recvTimestamp_ = msg.consumedTimestamp_;
        destId_ = msg.consumerId_;
        elapsedTimeInMs_ = calculateElapsedTimeInMs();
    }


    public void setRecvTimestamp(long time) {
        recvTimestamp_ = time;
        elapsedTimeInMs_ = calculateElapsedTimeInMs();
    }

    @Override
    public String getString() {
        return (String.format("[StatisticData]|MsgId %s|Producer %d|Consumer %d|Timestamp %d|TopicName %s|RecvTimestamp %d|ElapsedTimeMs %d ms|content %dMB \n",
                messageId_, sourceId_, destId_, timestamp_, topic_, recvTimestamp_, elapsedTimeInMs_, (content_ == null) ? 0 : content_.length));
    }

    public void log(Logger logger) {
        logger.debug(getString());
        logger.info(String.format("[STAT]|%s|%d|%d|%d|%s|%d|%d|%d",
                messageId_, sourceId_, destId_, timestamp_, topic_, recvTimestamp_, elapsedTimeInMs_, (content_ == null) ? 0 : content_.length));
    }

    private long calculateElapsedTimeInMs() {
        return (recvTimestamp_ - timestamp_);
    }
}
