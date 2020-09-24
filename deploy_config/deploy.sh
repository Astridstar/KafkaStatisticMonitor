#!/bin/bash

# deploy the configurations to the various environments
# deploy.sh $1 $2
# deploy.sh <deploy/clean> <forwarders/publishers> <topic>
#
# Configurations for custom app parameters
# <RECEIVED_DIR>
# <ENABLE_FORWARDER>
# <PUBLISHER_DURATION_SEC>
#
# <PRODUCER_COUNT>
# <CONSUMER_COUNT>
#
# <PUBLISHER_TOPIC>
# <PUBLISHER_CONSUMER_GROUP>
# <PAYLOAD>
# <MSG_PER_TICK>
# <TOTAL_MSG_TO_PUBLISH>
#
# <FORWARDER_TOPIC>
# <FOWARDER_CONSUMER_GROUP>
#
# Configurations for the producer.properties and consumer.properties
# <PRODUCER_BOOTSTRAP_SERVER>
# <CONSUMER_BOOTSTRAP_SERVER>
#
# Configurations for log4j
# <LOG_DIR>

current_dir=$(pwd)
exec_dir=/home/kafka/app/KafkaStatisticMonitor
base_dir=/home/kafka/app/clients


if [ $# -lt 3 ]
then 
	echo "Usage : $0 <deploy/clean> <forwarders/publishers> <topic>"
fi

topic_dir=$base_dir/$3
config_dir=$topic_dir/config
log_dir=$topic_dir/logs
recv_dir=$topic_dir/received
resource_dir=$exec_dir/resources

RECEIVED_DIR=$recv_dir
ENABLE_FORWARDER=false
PUBLISHER_DURATION_SEC=1000
PRODUCER_COUNT=10
CONSUMER_COUNT=10
#
PUBLISHER_TOPIC=$3
PUBLISHER_CONSUMER_GROUP=PUBLISHER_GROUP_$3
PUBLISHER_PAYLOAD=$resource_dir/file1M.txt
MSG_PER_TICK=1
TOTAL_MSG_TO_PUBLISH=12000
#Test- 20min, 1 msg/sec, 10 publishers, ~1M payloads

FORWARDER_TOPIC=$3
FOWARDER_CONSUMER_GROUP=FORWARDER_GROUP_$3
#
# Configurations for the producer.properties and consumer.properties
PUBLISHDER_PRODUCER_BOOTSTRAP_SERVER=kf1-svr-1:9092,kf1-svr-2:9092,kf1-svr-3:9092
PUBLISHDER_CONSUMER_BOOTSTRAP_SERVER=kf2-svr-1:9092,kf2-svr-2:9092,kf2-svr-3:9092
FORWARDER_PRODUCER_BOOTSTRAP_SERVER=kf2-svr-1:9092,kf2-svr-2:9092,kf2-svr-3:9092
FORWARDER_CONSUMER_BOOTSTRAP_SERVER=kf1-svr-1:9092,kf1-svr-2:9092,kf1-svr-3:9092
#
# Test producing and consuming from the same broker
#PUBLISHDER_PRODUCER_BOOTSTRAP_SERVER=kf2-svr-1:9092,kf2-svr-2:9092,kf2-svr-3:9092
#PUBLISHDER_CONSUMER_BOOTSTRAP_SERVER=kf2-svr-1:9092,kf2-svr-2:9092,kf2-svr-3:9092

function configure_publishers () {
	echo "Configuring publisher setup for topic $3"
	cp $current_dir/run.sh $topic_dir
	cp $current_dir/*.properties $config_dir
	
	cd $topic_dir
	
	APP_CONFIG_DIR=normal-producer.properties
	
	#run.sh
	sed -i "s|<RUN_SCRIPT_CONFIG_DIR>|$config_dir|g"  run.sh
	sed -i "s|<RUN_SCRIPT_EXE_DIR>|$exec_dir|g"  run.sh
	sed -i "s|<APP_CONFIG_DIR>|$APP_CONFIG_DIR|g"  run.sh
	chmod +x ./run.sh
	
	cd $config_dir	
	
	ENABLE_FORWARDER=false	
	
	#log4j.properties
	sed -i "s|<LOG_DIR>|$log_dir|g"  log4j.properties
	
	#producer.properties
	sed -i "s|<PRODUCER_BOOTSTRAP_SERVER>|$PUBLISHDER_PRODUCER_BOOTSTRAP_SERVER|g" producer.properties

	#consumer.properties
	sed -i "s|<CONSUMER_BOOTSTRAP_SERVER>|$PUBLISHDER_CONSUMER_BOOTSTRAP_SERVER|g" consumer.properties

	#normal-producer.properties
	sed -i "s|<RECEIVED_DIR>|$RECEIVED_DIR|g" $APP_CONFIG_DIR
	sed -i "s|<ENABLE_FORWARDER>|$ENABLE_FORWARDER|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_DURATION_SEC>|$PUBLISHER_DURATION_SEC|g" $APP_CONFIG_DIR
	sed -i "s|<PRODUCER_COUNT>|$PRODUCER_COUNT|g" $APP_CONFIG_DIR
	sed -i "s|<CONSUMER_COUNT>|$CONSUMER_COUNT|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_TOPIC>|$PUBLISHER_TOPIC|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_CONSUMER_GROUP>|$PUBLISHER_CONSUMER_GROUP|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_PAYLOAD>|$PUBLISHER_PAYLOAD|g" $APP_CONFIG_DIR
	sed -i "s|<MSG_PER_TICK>|$MSG_PER_TICK|g" $APP_CONFIG_DIR
	sed -i "s|<TOTAL_MSG_TO_PUBLISH>|$TOTAL_MSG_TO_PUBLISH|g" $APP_CONFIG_DIR	
		
	cd $current_dir
}

function configure_forwarders () {
	echo "Configuring forwarders setup for topic $3"
	cp $current_dir/run.sh $topic_dir
	cp $current_dir/*.properties $config_dir
	
	cd $topic_dir

	APP_CONFIG_DIR=forwarder.properties

	#run.sh
	sed -i "s|<RUN_SCRIPT_CONFIG_DIR>|$config_dir|g"  run.sh
	sed -i "s|<RUN_SCRIPT_EXE_DIR>|$exec_dir|g"  run.sh
	sed -i "s|<APP_CONFIG_DIR>|$APP_CONFIG_DIR|g"  run.sh
	chmod +x ./run.sh

	cd $config_dir
	
	ENABLE_FORWARDER=true

	#log4j.properties
	sed -i "s|<LOG_DIR>|$log_dir|g" log4j.properties
	
	#producer.properties
	sed -i "s|<PRODUCER_BOOTSTRAP_SERVER>|$FORWARDER_PRODUCER_BOOTSTRAP_SERVER|g" producer.properties

	#consumer.properties
	sed -i "s|<CONSUMER_BOOTSTRAP_SERVER>|$FORWARDER_CONSUMER_BOOTSTRAP_SERVER|g" consumer.properties
	
	#forwarder.properties
	sed -i "s|<RECEIVED_DIR>|$RECEIVED_DIR|g" $APP_CONFIG_DIR
	sed -i "s|<ENABLE_FORWARDER>|$ENABLE_FORWARDER|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_DURATION_SEC>|$PUBLISHER_DURATION_SEC|g" $APP_CONFIG_DIR
	sed -i "s|<PRODUCER_COUNT>|$PRODUCER_COUNT|g" $APP_CONFIG_DIR
	sed -i "s|<CONSUMER_COUNT>|$CONSUMER_COUNT|g" $APP_CONFIG_DIR
	sed -i "s|<FORWARDER_TOPIC>|$FORWARDER_TOPIC|g" $APP_CONFIG_DIR
	sed -i "s|<FOWARDER_CONSUMER_GROUP>|$FOWARDER_CONSUMER_GROUP|g" $APP_CONFIG_DIR
	sed -i "s|<PUBLISHER_PAYLOAD>|$PUBLISHER_PAYLOAD|g" $APP_CONFIG_DIR
	sed -i "s|<MSG_PER_TICK>|$MSG_PER_TICK|g" $APP_CONFIG_DIR
	sed -i "s|<TOTAL_MSG_TO_PUBLISH>|$TOTAL_MSG_TO_PUBLISH|g" $APP_CONFIG_DIR	
	
	cd $current_dir
}


case "$1" in

deploy) 
	echo "-------------------------------------"
	echo " Deploying configurations"
	echo "-------------------------------------"
	
	if [ ! -d "topic_dir" ] ; then
		mkdir $topic_dir;
	fi
	if [ ! -d "config_dir" ]; then
		mkdir $config_dir;
	fi	
	if [ ! -d "log_dir" ]; then
		mkdir $log_dir;
	fi	
	if [ ! -d "recv_dir" ]; then
		mkdir $recv_dir;
	fi
	
	if [ $2 = "publishers" ]; then
		configure_publishers 
	elif [ $2 = "forwarders" ]; then
		configure_forwarders
	else
		echo "Invalid mode"
	fi	
	;;
		
clean) 
	echo "-------------------------------------"
	echo " Cleaning configurations directory"
	echo "-------------------------------------"
	rm -rf $topic_dir
	;;
		
*) echo "Invalid options"

	;;

esac
