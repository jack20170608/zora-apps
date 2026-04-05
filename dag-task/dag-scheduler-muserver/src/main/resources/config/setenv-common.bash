#!/usr/bin/env bash

APP_ROOT="/home/"
APP_NAME=host-helper

JAVA_HOME="/home/jack/Runtime/jdk-25"

LOG_HOME="/home/jack/apps/host-helper-server/logs"

PROC_NAME="${APP_NAME}_10086"

mkdir -pv $LOG_HOME

STARTUP_TIMEOUT=300
STOP_TIMEOUT=300
PID_FILE="${LOG_HOME}/${PROC_NAME}.pid"
STARTUP_FILE="${LOG_HOME}/${PROC_NAME}.state"

JVM_ARG_1="-Xms512m -Xms512m"
JVM_ARG_5="-Xlog:gc*"


JVM_ARG_99="-jar ${}"


