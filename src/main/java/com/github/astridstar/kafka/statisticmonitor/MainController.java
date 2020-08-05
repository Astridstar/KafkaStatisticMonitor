package com.github.astridstar.kafka.statisticmonitor;

import com.github.astridstar.kafka.statistic.data.Configurator;
import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;
import com.github.astridstar.kafka.statistic.processing.MonitoringAgent;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

// Sample command line
// java -jar -Dlog4j.configuration=file:/home/aranel/projects/out/artifacts/kafka_statistics_monitor_jar/config/log4j.properties <_|
// kafka-statistics-monitor.jar <_|
// /home/aranel/projects/out/artifacts/kafka_statistics_monitor_jar/config <_|
public class MainController {

	private static final Logger logger = GeneralLogger.getLogger(MainController.class.getName());
	public static void main(String[] args) {
		parseArguments(args);
		
		logger.info("===================== MainController ready for action =====================");
		
        //String propertiesFile = Configurator.DEFAULT_CONFIG_DIR + "StatisticsMonitor.properties";
        if(!Configurator.load_properties(Configurator.CONFIG_FILE))
        {
        	logger.error("ERROR: Unable to load properties. Exiting program now");
        	return;
        }
             
        logger.info("Loading completed. Preparing the agents...");
        logger.info("Current MSG INDEX [" + Configurator.SESSION_ID_MSG_ID_PREFIX + "]");

        CountDownLatch latch = new CountDownLatch (1);
        MonitoringAgent agent = new MonitoringAgent();
        agent.configure();
        agent.start();

        Thread shutdownThread = new Thread(() -> {
            logger.info("Application shutdown initiated.");
            agent.cleanup ();
        });
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        try{
            latch.await ();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing");
        }
        logger.info("Application exiting...");

        /*
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        try {
            while(true) {
				Thread.sleep(1000); // in milliseconds
				String userInput = console.readLine();
				if (userInput == null) continue;

				if (userInput.contains("quit")) {
					break;
				}
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            agent.cleanup();
        }*/
	}

    private static void parseArguments(String[] args) {
        if(args == null || args.length == 0) {
            logger.error(Thread.currentThread().getStackTrace()[1].getMethodName() +
                    "() - Error: Parameter list is a null value");
        }

        for( String arg : args){
            Configurator.CONFIG_FILE = arg;
            try{
                Path p1 = Paths.get(Configurator.CONFIG_FILE);
                Configurator.DEFAULT_CONFIG_DIR = p1.getParent ().toString ();
            } catch (InvalidPathException e){
                e.printStackTrace ();
            }

        }
    }
}
