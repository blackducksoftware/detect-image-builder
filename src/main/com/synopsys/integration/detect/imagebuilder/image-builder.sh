#!/bin/bash

VERSION=1.0

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}
IMAGE_ORG=blackducksoftware

# Java Version to support
JAVA_VERSION=11

# Detect Versions to support
DETECT_VERSIONS=( 7.0.0 6.9.1 )

DETECT_BASE_IMAGE_DOCKERFILE=detect-base-dockerfile

# NOTE: When supplying arguments to this function, ORDER MATTERS.
#   If a package manager doesn't require one of the args (ex. npm doesn't specify a PKG_MGR_VERSION since we're only using the version alpine supports) provide an empty string in its place
function buildPkgMgrImage() {
    local IMAGE_NAME=$1
    local ORG=$2
    local DETECT_VERSION=$3
    local PKG_MGR_VERSION=$4
    local DOCKERFILE_NAME=$5

    docker rmi -f ${IMAGE_NAME} \
    && docker build \
    --build-arg "ORG=${ORG}" \
    --build-arg "DETECT_VERSION=${DETECT_VERSION}" \
    --build-arg "PKG_MGR_VERSION=${PKG_MGR_VERSION}" \
    -t ${IMAGE_NAME} \
    -f ${DOCKERFILE_NAME} \
    .
}

function pushImage() {
    # Stop execution on an error
    set -e

    local IMAGE_NAME=$1
    # Login info is sourced from the build environment
    docker login --username ${DOCKER_INT_BLACKDUCK_USER} --password ${DOCKER_INT_BLACKDUCK_PASSWORD}
    docker push ${IMAGE_NAME}
    docker logout

    set +e
}

for detectVersion in "${DETECT_VERSIONS[@]}";
    do
        # Build Detect Base Image
        IMAGE_NAME=${IMAGE_ORG}/detect:${detectVersion}
        docker build \
            --build-arg "DETECT_VERSION=${detectVersion}" \
            -t ${IMAGE_NAME} \
            -f ${DETECT_BASE_IMAGE_DOCKERFILE} \
            .

    pushImage ${IMAGE_NAME}

    # Build Package Manager Images

    # Gradle
    GRADLE_VERSIONS=( 6.8.2 )
    GRADLE_DOCKERFILE=gradle-dockerfile
    for gradleVersion in "${GRADLE_VERSIONS[@]}";
        do
            IMAGE_NAME=${IMAGE_ORG}/detect:${detectVersion}-gradle-${gradleVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${detectVersion} ${gradleVersion} ${GRADLE_DOCKERFILE}
            pushImage ${IMAGE_NAME}
    done

    # Maven
    MAVEN_VERSIONS=( 3.8.1 )
    MAVEN_DOCKERFILE=maven-dockerfile
    for mavenVersion in "${MAVEN_VERSIONS[@]}";
        do
            IMAGE_NAME=${IMAGE_ORG}/detect:${detectVersion}-maven-${mavenVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${detectVersion} ${mavenVersion} ${MAVEN_DOCKERFILE}
            pushImage ${IMAGE_NAME}
    done

    # Npm
    NPM_DOCKERFILE=npm-dockerfile
    IMAGE_NAME=${IMAGE_ORG}/detect:${detectVersion}-npm
    buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${detectVersion} "" ${NPM_DOCKERFILE}
    pushImage ${IMAGE_NAME}

done