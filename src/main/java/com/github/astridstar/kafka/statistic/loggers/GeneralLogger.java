package com.github.astridstar.kafka.statistic.loggers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class GeneralLogger {
    private static Logger m_logger = LoggerFactory.getLogger(GeneralLogger.class);

    public static Logger getDefaultLogger(){
        return m_logger;
    }

    public static Logger getLogger(String name){
        return LoggerFactory.getLogger(name);
    }
}
