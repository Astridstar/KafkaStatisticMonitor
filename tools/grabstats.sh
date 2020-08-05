#!/bin/bash

current_dir=$(pwd)
logs_dir=/home/aranel/projects/logs

cd $logs_dir

#<search> <file> <column>
#<logfile> <output file>

egrep "\[MSG\]|\[STAT\]|New stats window|FINAL\]|TOTAL\]" $1 > $2 


cd $current_dir
