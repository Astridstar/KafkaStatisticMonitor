package com.github.astridstar.kafka.statistic.data;

import org.slf4j.Logger;

public class StatisticRecorder {
    private double m_accProcessedDataSizeInKb ;
    private long m_processedDataCounter;
    private long m_accLatencies;

    private long m_startTimeInMs;
    private long m_endTimeInMs;

    private double m_avgProcessedDataKBPerSec;
    private double m_avgLatency;
    private double m_durationInSec;
    private double m_durationInMin;
    private double m_durationInHour;

    public StatisticRecorder() {
        m_accProcessedDataSizeInKb = 0;
        m_processedDataCounter = 0;
        m_startTimeInMs = 0;

        m_avgProcessedDataKBPerSec = 0;
        m_avgLatency = 0;
        m_durationInHour = 0;
    }

    public void startCalculating() {
        m_startTimeInMs = System.currentTimeMillis();
    }

    public void stopCalculating() {
        m_endTimeInMs = System.currentTimeMillis();
        calculate(m_endTimeInMs);
    }

    public void update(long sizeInBytes, long elapsedTimeInMs) {
        m_endTimeInMs = System.currentTimeMillis();
        m_accProcessedDataSizeInKb += ((double)sizeInBytes) / 1024;
        ++m_processedDataCounter;
        m_accLatencies += elapsedTimeInMs;
        calculate(m_endTimeInMs);
    }
    private void calculate(long endtime) {
        if(m_processedDataCounter == 0)
            return;

        m_durationInSec = ((double)(endtime - m_startTimeInMs)) /1000;
        m_durationInMin = ((double) m_durationInSec) / 60;
        m_durationInHour = ((double) m_durationInMin) /60;

        m_avgProcessedDataKBPerSec = m_accProcessedDataSizeInKb / (((double)(endtime - m_startTimeInMs))/1000);
        m_avgLatency = ((float)m_accLatencies) / m_processedDataCounter;
    }


    public void print(Logger logger) {
        logger.info("-------------------------------------------------------------------------------------------");
        logger.info(String.format("  Avg DataSize(%.5f KB/sec)|Avg Xsmit Time(%.2f ms)|Volume(%d)|MSG Size(%.5f KB)|Duration-h/m/s(%.3f)/(%.3f)/(%.3f)",
                m_avgProcessedDataKBPerSec, m_avgLatency, m_processedDataCounter, m_accProcessedDataSizeInKb, m_durationInHour, m_durationInMin, m_durationInSec));
        logger.info("-------------------------------------------------------------------------------------------");
    }

    public void logStatistics(Logger logger) {
        logger.info("-----------------------------------------------------");
        logger.info(String.format("  Summary Avg data           (%.5f KB/sec)", m_avgProcessedDataKBPerSec));
        logger.info(String.format("  Summary Avg latency        (%.2f ms)", m_avgLatency));
        logger.info(String.format("  Summary Volume of messages (%d) ", m_processedDataCounter));
        logger.info(String.format("  Summary Test duration      (%.5f hrs) or (%.5f min) or (%.5f secs) ", m_durationInHour, m_durationInMin, m_durationInSec));
        logger.info("-----------------------------------------------------");
    }
}
