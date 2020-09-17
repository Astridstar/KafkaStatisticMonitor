package com.github.astridstar.kafka.statistic.processing;

import com.github.astridstar.kafka.statistic.data.KafkaMessage;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

public class ProducerCallbackImpl implements Callback {
    private final long startTime;
    private final KafkaMessage kMessage;

    public ProducerCallbackImpl(long startTime, KafkaMessage msg) {
        this.startTime = startTime;
        this.kMessage = msg;
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
        if (exception == null) {
            GeneralLogger.getDefaultLogger().debug(
                    "[PUBLISHED] Topic[" + metadata.topic ()
                            + "] Partition[" + metadata.partition()
                            + "] Offset[" + metadata.offset()
                            + "] Timestamp[" + metadata.timestamp()
                            + "] Latency[" + latency + " ms"
                            + "] Size[" + metadata.serializedValueSize()
                            + "] " + "\n" + kMessage.getString());
        } else {
            GeneralLogger.getDefaultLogger().error("[PUBLISHING-FAILED]" + kMessage.getString(), exception);
        }
    }
}
