#!/bin/bash

if [ $# -lt 1 ]
then 
	echo "Usage : $0 <topic>"
	exit
fi

current_dir=$(pwd)
kafka_dir=/home/kafka/kafka

cd $kafka_dir
bin/kafka-producer-perf-test.sh --producer.config config/producer.properties --topic $1 --num-records 90000 --payload-delimiter @ --throughput 10 --print-metrics --record-size 1048576
cd $current_dir

