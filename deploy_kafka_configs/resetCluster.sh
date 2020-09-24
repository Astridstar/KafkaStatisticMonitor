#!/bin/bash

# resetCluster <broker/keeper>
current_dir=$(pwd)
kafka_dir=/home/kafka/kafka
kafka_config=$kafka_dir/logs

keeper_data_path=/data/zookeeper
data_path_1=/data1/kafka
data_path_2=/data2/kafka


if [ $# -lt 1 ]
then 
	echo "Usage : $0 <broker/keeper>"
fi

function clean_broker () {
	echo "-------------------------------------"
	echo " Cleaning broker"
	echo "-------------------------------------"
	
	echo " ** Stopping kafka service **"
	sudo systemctl stop kafka;
	
	echo " ** Waiting 30s for kafka service to stop **"
	sleep 30
	
	echo " ** Removing broker data files **"
	rm -rf $data_path_1/*
	rm -rf $data_path_2/*
	
	echo " ** Removing broker application logs **"
	rm $kafka_config/*
}

function clean_zookeeper () {
	echo "-------------------------------------"
	echo " Cleaning zookeeper"
	echo "-------------------------------------"
	
	echo " ** Stopping zookeeper service **"
	sudo systemctl stop zookeeper;
	
	echo " ** Waiting 30s for zookeeper service to stop **"
	sleep 30
	
	echo " ** Removing zookeeper data files **"
	rm -rf $keeper_data_path/*
	
	echo " ** Removing zookeeper application logs **"
	rm $kafka_config/*
}

case "$1" in

broker) 
	clean_broker	
	;;		
keeper) 
	clean_zookeeper
	;;		
*) echo "Invalid options"
	;;

esac