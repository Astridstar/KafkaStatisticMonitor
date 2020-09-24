#!/bin/bash

if [ $# -lt 1 ]
then 
	echo "Usage : $0 <topic>"
	exit
fi

current_dir=$(pwd)
kafka_dir=/home/kafka/kafka

cd $kafka_dir
bin/kafka-consumer-perf-test.sh --bootstrap-server kf2-svr-1:9092,kf2-svr-2:9092,kf2-svr-3:9092 --consumer.config config/consumer.properties --topic $1 --print-metrics  --messages 90000 --show-detailed-stats
cd $current_dir

