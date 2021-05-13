#!/bin/sh

# Get args
while getopts d:v: flag
do
    case "${flag}" in
        d) mavenFilesDir=${OPTARG};;
        v) mavenVersion=${OPTARG};;
    esac
done

mavenMajorVersion=${mavenVersion:0:1}

mavenTarName=maven.tar.gz

cd ${mavenFilesDir}/${mavenVersion} \
    && curl -L https://archive.apache.org/dist/maven/maven-${mavenMajorVersion}/${mavenVersion}/binaries/apache-maven-${mavenVersion}-bin.tar.gz -o ${mavenTarName} \
    && tar -xzf ${mavenTarName} \
    && rm -f ${mavenTarName} \
    && ln -s apache-maven-${mavenVersion} maven