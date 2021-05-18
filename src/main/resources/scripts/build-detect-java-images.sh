#!/bin/bash

# Get args
while getopts e:i:d flag
do
    case "${flag}" in
        e) DETECT_FILES_PATH=${OPTARG};;
        i) DETECT_VERSION=${OPTARG};;
        d) DOCKERFILE_PATH=${OPTARG};;
        f) DOCKERFILE_NAME=${OPTARG};;
        j) JAVA_VERSION=${OPTARG};;
        v) IMAGE_VERSION=${OPTARG};;
        o) ORG=${OPTARG};;
    esac
done

cd ${DOCKERFILE_PATH} \
    && docker build \
    --build-arg "DETECT_FILES_DIR=${DETECT_FILES_PATH}" \
    --build-arg "DETECT_JAR_NAME=synopsys-detect-${DETECT_VERSION}.jar" \
    --build-arg "JAVA_VERSION=11" \
    -t ${ORG}/detect-${DETECT_VERSION}-java-${JAVA_VERSION}:${IMAGE_VERSION} \
    -f ${DOCKERFILE_NAME} \
    .