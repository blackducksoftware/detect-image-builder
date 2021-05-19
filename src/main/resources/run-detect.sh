#!/bin/bash

# This script will be run as the entrypoint for detect containers

DETECT_JAR=synopsys-detect.jar

SOURCE_DIR=source

DISPLAY_HELP=false

# Get Args

ARGS=""

while [[ $# -gt 0 ]]
do
arg="$1"

case $arg in
    -h|--help)
    DISPLAY_HELP=true
    break
    ;;
    *) # append to args
    ARGS="${ARGS} ${arg}"
    shift
    ;;
esac
done

if [[ ${DISPLAY_HELP} == "true" ]]
then
    CMD="java -jar /${DETECT_JAR} -hv"
else
    CMD="java -jar /${DETECT_JAR} --detect.source.path=/${SOURCE_DIR} ${ARGS}"
fi

echo "Running ${CMD}"
eval ${CMD}
RESULT=$?
echo "Result code of ${RESULT}, exiting."
exit ${RESULT}
