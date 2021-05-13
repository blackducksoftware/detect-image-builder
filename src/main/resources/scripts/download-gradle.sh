#!/bin/sh

# Get args
while getopts d:v: flag
do
    case "${flag}" in
        d) gradleFilesDir=${OPTARG};;
        v) gradleVersion=${OPTARG};;
    esac
done

gradleZipName=gradle-${gradleVersion}-bin.zip

cd ${gradleFilesDir}/${gradleVersion} \
            && curl -L https://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip -o ${gradleZipName} \
            && unzip -o ${gradleZipName} \
            && rm ${gradleZipName} \
            && ln -s gradle-${gradleVersion} gradle
