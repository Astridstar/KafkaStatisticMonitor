#!/bin/bash

# deploy the configurations to the various environments
# deploy.sh $1 $2
# deploy.sh <CLUSTER_ID> <BROKER_ID>
#

current_dir=$(pwd)
kafka_dir=/home/kafka/kafka
kafka_config=$kafka_dir/config

if [ $# -lt 2 ]
then 
	echo "Usage : $0 <CLUSTER_ID> <BROKER_ID>"
	exit
fi

CLUSTER_ID=$1
BROKER_ID=$2

function configure_broker () {
	echo "Configuring Kafka broker server.properties for cluster $CLUSTER_ID and broker $BROKER_ID"

	# backup existing configuration server.properties
	echo "Creating a ocpy of existing up Kafka broker server.properties"
	cp $kafka_config/server.properties $kafka_config/server.properties_bak
	
	# copy template server.properties to kafka directory
	echo "Copy template server.properties to kafka config directory"
	cp $current_dir/server.properties $kafka_config
	
	# Navigate to kafka configuration directory
	cd  $kafka_config
	
	# Replace cluster id and broker id respective 
	echo "Replacing cluster id ($CLUSTER_ID) and broker id ($BROKER_ID)in server.properties "
	sed -i "s|<CLUSTER_ID>|$CLUSTER_ID|g"  server.properties
	sed -i "s|<BROKER_ID>|$BROKER_ID|g"  server.properties
	
	# Navigate back to original directory
	cd $current_dir	
}

configure_broker

