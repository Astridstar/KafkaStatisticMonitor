#!/bin/bash

current_dir=$(pwd)
config_dir=<RUN_SCRIPT_CONFIG_DIR>
exec_dir=<RUN_SCRIPT_EXE_DIR>
application_config_dir=<APP_CONFIG_DIR>

cd $exec_dir

java -jar -Dlog4j.configuration=file:$config_dir/log4j.properties kafka-statistics-monitor.jar $config_dir/$application_config_dir

cd $current_dir
