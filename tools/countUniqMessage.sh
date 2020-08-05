#!/bin/bash

current_dir=$(pwd)
logs_dir=/home/aranel/projects/logs

cd $logs_dir

#<search> <file> <column>

grep $1 $2 | cut -d '|' -f $3 | sort -u | wc -l 

cd $current_dir