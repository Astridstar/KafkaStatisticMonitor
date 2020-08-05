package com.github.astridstar.kafka.statistic.data;

import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

public class Configurator {

    private static final String  KEY_METRICS_REPORTING_INTERVAL_MS="metrics.reporting.interval.ms";
    private static final String  KEY_FORWARDER_FUNCTION_ENABLED="fowarder.function.enabled";
    private static final String  KEY_PUBLISHING_DURATION_SEC="publishing.duration.sec";

    private static final String  KEY_NUM_PRODUCERS = "producers.num";
    private static final String  KEY_PRODUCER_PREFIX = "producers.prefix";
    private static final String  KEY_PRODUCERS_REST_DURATION_MS="producers.rest.duration.ms";
    private static final String  KEY_PRODUCERS_PUBLISH_WITH_KEY="producers.publish.with.key";
    private static final String  KEY_PRODUCERS_SEND_TRANSACTIONS="producers.send.transactions";

    private static final String  SUFFIX_PRODUCER_ID="-id";
    private static final String  SUFFIX_PRODUCER_TOPIC="-topic";
    private static final String  SUFFIX_PRODUCER_INTERVAL_MSG_COUNT="-interval-msg-count";
    private static final String  SUFFIX_PRODUCER_PAYLOAD_FILE="-payload-file";
    private static final String  SUFFIX_PRODUCER_MAX_MSG_TO_PUBLISH="-max-msg-to-publish";

    private static final String  KEY_NUM_CONSUMERS = "consumers.num";
    private static final String  KEY_CONSUMER_PREFIX = "consumers.prefix";
    private static final String  KEY_DESTINATION_FOLDER="consumers.destination.folder";

    private static final String  SUFFIX_CONSUMER_TOPIC="-topic";
    private static final String  SUFFIX_CONSUMER_PRODUCER_ID="-producer-id";
    private static final String  SUFFIX_CONSUMER_GROUP="-group-id";

    private static final String  DEFAULT_CONSUMER_GROUP="CONSUMER_GROUP_1";

    public static final String  DEFAULT_MSG_ID_SEPARATOR="-";
    public static final String  DEFAULT_PAYLOAD_MARKER="@";
    public static final String  DEFAULT_LOGGER_GROUP_PREFIX = "GROUP_";
    public static final int DEFAULT_PRODUCER_INTERVAL_MSG_COUNT =1;

    public static final String  SESSION_ID_MSG_ID_PREFIX = new SimpleDateFormat("yyyyMMdd-HHmmss-").format(new Date());

    public static String DEFAULT_CONFIG_DIR = new String(System.getProperty("user.dir") + File.separator);
    public static String CONFIG_FILE = new String(System.getProperty("user.dir") + File.separator + "StatisticsMonitor.properties");

    private static int m_numOfProducers = 0;
    private static int m_numOfConsumers = 0;

    private static String m_producersPrefix = "producer-";
    private static String m_consumersPrefix = "consumer-";

    private static String m_consumerDestinationFolder = "";
    private static long m_producersRestDurationInMs = 10000;

    private static boolean m_bToPublishWithKey = false;
    private static long m_reportingIntervalInMs = 10000;
    private static boolean m_bIsForwardingEnabled = false;
    private static long m_publishingDurationInSec = 10;
    private static boolean m_bIsTransactionsEnabled = false;

    private static HashMap<Integer, ProducerInfo> m_producerMap = new HashMap<Integer, ProducerInfo>();
    private static HashMap<Integer, ConsumerInfo> m_consumerMap = new HashMap<Integer, ConsumerInfo>();

    private static Properties properties_ = null;

    private Configurator() {}

    /**
     * @return the m_numOfProducers
     */
    public static int getNumOfProducers() {
        return m_numOfProducers;
    }
    /**
     * @return the m_numOfConsumers
     */
    public static int getNumOfConsumers() {
        return m_numOfConsumers;
    }
    /**
     * @return the m_consumerDestinationFolder
     */
    public static String getConsumerDestinationFolder(){
        return m_consumerDestinationFolder;
    }
    /**
     * @return the m_producersRestDurationInMs
     */
    public static long getProducersRestDurationInMs(){
        return m_producersRestDurationInMs;
    }
    /**
     * @return the m_bToPublishWithKey
     */
    public static boolean getBToPublishWithKey(){
        return m_bToPublishWithKey;
    }
    /**
     * @return the m_reportingIntervalInMs
     */
    public static long getReportingIntervalInMs(){
        return m_reportingIntervalInMs;
    }
    /**
     * @return the m_bIsForwardingEnabled
     */
    public static boolean getBIsForwardingEnabled(){
        return m_bIsForwardingEnabled;
    }
    /**
     * @return the m_publishingDurationInSec
     */
    public static long getPublishingDurationInSec(){
        return m_publishingDurationInSec;
    }
    /**
     * @return the m_bIsTransactionsEnabled
     */
    public static boolean getBIsTransactionsEnabled(){
        return m_bIsTransactionsEnabled;
    }
    public static boolean load_properties(String fileName)
    {
        boolean isLoadCompleted = false;
        // open the property file.
        properties_ = new Properties();

        InputStream iStream = null;
        try
        {
            iStream = new FileInputStream(fileName);
        }
        catch (FileNotFoundException e)
        {
            GeneralLogger.getDefaultLogger().error("Couldn't find [" + fileName + "]" + e.getMessage());
        } //ClassLoader.getSystemResourceAsStream(fileName);

        if (iStream == null)
        {
            GeneralLogger.getDefaultLogger().error("Couldn't find " + fileName + " in classpath.");
        }
        else
        {
            // load the properties file
            try {
                properties_.load(iStream);
                isLoadCompleted = true;
            } catch (IOException ioe) {
                GeneralLogger.getDefaultLogger().error("Fail to load " + fileName + ". Exception: ", ioe);
            } finally {
                // close the stream
                try
                {
                    iStream.close();
                }
                catch (IOException ioe)
                {
                    // ignore
                }
            }
        }

        if(!isLoadCompleted)
            return false;

        return loadLocalConfigSettings(properties_);
    }


    public static boolean loadLocalConfigSettings(Properties theProperties ) {
        String value;

        //KEY_DESTINATION_FOLDER
        value = theProperties.getProperty(Configurator.KEY_DESTINATION_FOLDER);
        if(value != null && !value.isEmpty())
        {
            m_consumerDestinationFolder = value + "/" + SESSION_ID_MSG_ID_PREFIX + "records";

            File f=new File(m_consumerDestinationFolder);
            if (!f.exists()) {
                final boolean b = f.mkdirs();
            }

            //Path path = Paths.get(m_consumerDestinationFolder);
            //if(path == null)
            //Path directories = Files.createDirectories(m_consumerDestinationFolder);
            GeneralLogger.getDefaultLogger().info(KEY_DESTINATION_FOLDER + ":" + Configurator.m_consumerDestinationFolder);
        }
        else
        {
            m_consumerDestinationFolder = "";
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_DESTINATION_FOLDER
                    + ". Using default values:" + m_consumerDestinationFolder);
        }

        // Get the number of producers
        value = theProperties.getProperty(Configurator.KEY_NUM_PRODUCERS);
        if(value != null && !value.isEmpty())
        {
            m_numOfProducers = parseInt(value);
            GeneralLogger.getDefaultLogger().info(KEY_NUM_PRODUCERS + ":" + Configurator.m_numOfProducers);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_NUM_PRODUCERS);
            return false;
        }

        // Get the producer prefix
        value = theProperties.getProperty(Configurator.KEY_PRODUCER_PREFIX);
        if(value != null && !value.isEmpty())
        {
            m_producersPrefix = value;
            GeneralLogger.getDefaultLogger().info(KEY_PRODUCER_PREFIX + ":" + Configurator.m_producersPrefix);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_PRODUCER_PREFIX);
            return false;
        }

        //KEY_PRODUCERS_REST_DURATION_MS
        value = theProperties.getProperty(Configurator.KEY_PRODUCERS_REST_DURATION_MS);
        if(value != null && !value.isEmpty())
        {
            m_producersRestDurationInMs = parseLong(value);
            GeneralLogger.getDefaultLogger().info(KEY_PRODUCERS_REST_DURATION_MS + ":" + Configurator.m_numOfProducers);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_PRODUCERS_REST_DURATION_MS
                    + ". Defaulting to " + m_producersRestDurationInMs + "ms");
        }

        // Get the number of consumers
        value = theProperties.getProperty(Configurator.KEY_NUM_CONSUMERS);
        if(value != null && !value.isEmpty())
        {
            m_numOfConsumers = parseInt(value);
            GeneralLogger.getDefaultLogger().info(KEY_NUM_CONSUMERS + ":" + Configurator.m_numOfConsumers);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_NUM_CONSUMERS);
            return false;
        }

        // Get the consumer prefix
        value = theProperties.getProperty(Configurator.KEY_CONSUMER_PREFIX);
        if(value != null && !value.isEmpty())
        {
            m_consumersPrefix = value;
            GeneralLogger.getDefaultLogger().info(KEY_CONSUMER_PREFIX + ":" + Configurator.m_consumersPrefix);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_CONSUMER_PREFIX);
            return false;
        }

        //KEY_PRODUCERS_PUBLISH_WITH_KEY
        value = theProperties.getProperty(Configurator.KEY_PRODUCERS_PUBLISH_WITH_KEY);
        if(value != null && !value.isEmpty())
        {
            if(value.equalsIgnoreCase("true") || Objects.equals(value, "1")) m_bToPublishWithKey = true;
            GeneralLogger.getDefaultLogger().info(KEY_PRODUCERS_PUBLISH_WITH_KEY + ":" + Configurator.m_bToPublishWithKey);
        } else {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_PRODUCERS_PUBLISH_WITH_KEY
                    + ". Defaulting to " + m_bToPublishWithKey);
        }

        // KEY_METRICS_REPORTING_INTERVAL_MS
        value = theProperties.getProperty(Configurator.KEY_METRICS_REPORTING_INTERVAL_MS);
        if(value != null && !value.isEmpty())
        {
            m_reportingIntervalInMs = parseLong(value);
            GeneralLogger.getDefaultLogger().info(KEY_METRICS_REPORTING_INTERVAL_MS + ":" + Configurator.m_reportingIntervalInMs);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_METRICS_REPORTING_INTERVAL_MS
                    + ". Defaulting to " + m_reportingIntervalInMs);
        }

        //KEY_FORWARDER_FUNCTION_ENABLED
        value = theProperties.getProperty(Configurator.KEY_FORWARDER_FUNCTION_ENABLED);
        if(value != null && !value.isEmpty())
        {
            if(value.equalsIgnoreCase("true") || Objects.equals(value, "1"))
                m_bIsForwardingEnabled = true;

            GeneralLogger.getDefaultLogger().info(KEY_FORWARDER_FUNCTION_ENABLED + ":" + Configurator.m_bIsForwardingEnabled);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_FORWARDER_FUNCTION_ENABLED
                    + ". Defaulting to " + m_bIsForwardingEnabled);
        }

        value = theProperties.getProperty(Configurator.KEY_PUBLISHING_DURATION_SEC);
        if(value != null && !value.isEmpty())
        {
            m_publishingDurationInSec = parseLong(value);
            GeneralLogger.getDefaultLogger().info(KEY_PUBLISHING_DURATION_SEC + ":" + Configurator.m_publishingDurationInSec);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_PUBLISHING_DURATION_SEC
                    + ". Defaulting to " + m_publishingDurationInSec);
        }

        //KEY_PRODUCERS_SEND_TRANSACTIONS
        value = theProperties.getProperty(Configurator.KEY_PRODUCERS_SEND_TRANSACTIONS);
        if(value != null && !value.isEmpty())
        {
            if(value.equalsIgnoreCase("true") || Objects.equals(value, "1"))
                m_bIsTransactionsEnabled = true;

            GeneralLogger.getDefaultLogger().info(KEY_PRODUCERS_SEND_TRANSACTIONS + ":" + Configurator.m_bIsTransactionsEnabled);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + KEY_PRODUCERS_SEND_TRANSACTIONS
                    + ". Defaulting to " + m_bIsTransactionsEnabled);
        }

        for(int i = 1; i <= m_numOfProducers; i++)
        {
            Configurator.loadProducer(i, theProperties);
        }

        for(int i = 1; i <= m_numOfConsumers; i++)
        {
            Configurator.loadConsumer(i, theProperties);
        }

        return true;
    }

    private static long parseLong(String sText)
    {
        // Nothing to validate : sText
        long iRetVal = 0;
        try
        {
            iRetVal = Long.parseLong(sText);
        }
        catch (NumberFormatException ne)
        {
            GeneralLogger.getDefaultLogger().trace("Exception: " + ne.getMessage());
        }
        return iRetVal;
    }

    private static int parseInt(String sText)
    {
        // Nothing to validate : sText
        int iRetVal = 0;
        try
        {
            iRetVal = Integer.parseInt(sText);
        }
        catch (NumberFormatException ne)
        {
            GeneralLogger.getDefaultLogger().trace("Exception: " + ne.getMessage());
        }
        return iRetVal;
    }

    /**
     * Parse to boolean from string
     */
    public static boolean parseBoolean(String sText)
    {
        try {
            return Boolean.parseBoolean(sText);
        } catch (NumberFormatException ne) {
            GeneralLogger.getDefaultLogger().trace("Exception: " + ne.getMessage());
            throw ne;
        }
    }

    public static ProducerInfo getProducerInformation(int i)
    {
        return m_producerMap.get(i);
    }

    public static ConsumerInfo getConsumerInformation(int i)
    {
        return m_consumerMap.get(i);
    }

    private static boolean loadProducer(int id, Properties theProperties)
    {
        ProducerInfo info = new ProducerInfo(id, "");

        String key = Configurator.m_producersPrefix + id + SUFFIX_PRODUCER_TOPIC;
        String value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty()) {
            info.topic_ = value;
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        } else {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + key);
            return false;
        }

        key = Configurator.m_producersPrefix + id + SUFFIX_PRODUCER_INTERVAL_MSG_COUNT;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty()) {
            info.intervalMessageCount_ = parseInt(value);
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        } else {
            GeneralLogger.getDefaultLogger().error(
                    "Unable to load : " + key +
                            ". Defaulting to " + DEFAULT_PRODUCER_INTERVAL_MSG_COUNT );
            info.intervalMessageCount_ = DEFAULT_PRODUCER_INTERVAL_MSG_COUNT;
        }

        key = Configurator.m_producersPrefix + id + SUFFIX_PRODUCER_PAYLOAD_FILE;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty()) {
            info.payloadFile_ = value;
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        } else {
            GeneralLogger.getDefaultLogger().error(
                    "Unable to load : " + key +
                            ". Defaulting to no payload");
        }

        key = Configurator.m_producersPrefix + id + SUFFIX_PRODUCER_ID;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty()) {
            info.id_ = Integer.parseInt (value);
            GeneralLogger.getDefaultLogger().info(key + "=" + value);
        } else {
            GeneralLogger.getDefaultLogger().error(
                    "Unable to load : " + key +
                            ". Defaulting to " + String.valueOf(info.id_));
        }

        //SUFFIX_PRODUCER_MAX_MSG_TO_PUBLISH
        key = Configurator.m_producersPrefix + id + SUFFIX_PRODUCER_MAX_MSG_TO_PUBLISH;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty()) {
            info.maxMessageToPublish_ = parseLong (value);
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        } else {
            GeneralLogger.getDefaultLogger().error(
                    "Unable to load : " + key +
                            ". Defaulting to " + SUFFIX_PRODUCER_MAX_MSG_TO_PUBLISH );
            info.maxMessageToPublish_ = 1000;
        }

        m_producerMap.put( id , info);
        return true;
    }

    private static boolean loadConsumer(int id, Properties theProperties)
    {
        String key = "";
        ConsumerInfo consumerInfo = new ConsumerInfo(id, "", 0, "");

        key = Configurator.m_consumersPrefix + id + SUFFIX_CONSUMER_TOPIC;
        String value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty())
        {
            consumerInfo.topic_ = value;
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + key);
            return false;
        }

        key = Configurator.m_consumersPrefix + id + SUFFIX_CONSUMER_PRODUCER_ID;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty())
        {
            consumerInfo.producerId_ = parseInt(value);
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error("Unable to load : " + key);
            return false;
        }

        key = Configurator.m_consumersPrefix + id + SUFFIX_CONSUMER_GROUP;
        value = theProperties.getProperty(key);
        if(value != null && !value.isEmpty())
        {
            consumerInfo.groupId_ = value;
            GeneralLogger.getDefaultLogger().info( key + "=" + value);
        }
        else
        {
            GeneralLogger.getDefaultLogger().error(
                    "Unable to load : " + key +
                            ". Defaulting to " + DEFAULT_CONSUMER_GROUP);
            consumerInfo.groupId_ = DEFAULT_CONSUMER_GROUP;
        }

        m_consumerMap.put(Integer.valueOf(id), consumerInfo);
        return true;
    }
}