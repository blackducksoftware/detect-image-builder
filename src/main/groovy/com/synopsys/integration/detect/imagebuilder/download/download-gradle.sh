#!/bin/bash

cd ${PKG_MGR_FILES_PATH}/gradle

# When to set GRADLE_VERSION?
if [[ ! -f "gradle" ]]; then
    curl -L https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip \
    && rm gradle-${GRADLE_VERSION}-bin.zip
fi