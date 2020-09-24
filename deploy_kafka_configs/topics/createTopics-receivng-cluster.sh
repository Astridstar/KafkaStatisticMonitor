#!/bin/sh

if [ $# -lt 1 ]
then 
	echo "Usage : $0 <CLUSTER_ID>"
	exit
fi

current_dir=$(pwd)
exec_dir=/home/kafka/kafka/bin
bootstrap_servers=kf$1-svr-1:9092,kf$1-svr-2:9092,kf$1-svr-3:9092

cd $exec_dir

$exec_dir/kafka-topics.sh --bootstrap-server $bootstrap_servers --create --topic POCTOPIC1 --replication-factor 3 --partitions 10 --config min.insync.replicas=2 --config max.message.bytes=5242880
$exec_dir/kafka-topics.sh --bootstrap-server $bootstrap_servers --create --topic POCTOPIC2 --replication-factor 3 --partitions 10 --config min.insync.replicas=2 --config max.message.bytes=5242880

cd $current_dir
