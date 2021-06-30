#!/bin/bash

### Config

# If a variable is unset, stop and exit
set -u

# cd to same directory that script is in
cd "$(dirname "${BASH_SOURCE[0]}")"

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}

ORG=blackducksoftware

DETECT_DOCKERFILE=detect-dockerfile
DETECT_LITE_DOCKERFILE=detect-lite-dockerfile

## Versions to support

# Alpine
ALPINE_VERSION=3.13

# Java
JAVA_VERSION=11

# Detect
DETECT_VERSIONS=( 7.0.0 6.9.1 )

# Handle toggle for running release
shopt -s nocasematch
if [[ $# == 1 && $1 == true ]]; then
	RELEASE_BUILD=TRUE
else
	RELEASE_BUILD=FALSE
fi
shopt -u nocasematch

# set -u is not working inside functions
# reset expected environment variables here to get benefits of set -u
#DOCKER_REGISTRY_SIG="${DOCKER_REGISTRY_SIG}"
#ARTIFACTORY_DEPLOYER_USER="${ARTIFACTORY_DEPLOYER_USER}"
#ARTIFACTORY_DEPLOYER_PASSWORD="${ARTIFACTORY_DEPLOYER_PASSWORD}"
#DOCKER_INT_BLACKDUCK_USER="${DOCKER_INT_BLACKDUCK_USER}"
#DOCKER_INT_BLACKDUCK_PASSWORD="${DOCKER_INT_BLACKDUCK_PASSWORD}"

### Functions

function removeImage() {
    local IMAGE_NAME=$1

    if docker image inspect "${IMAGE_NAME}" 1> /dev/null 2>&1; then
        docker rmi -f "${IMAGE_NAME}"
    fi
}

function logAndRun() {
    # shellcheck disable=SC2145
    echo "[$(date)] Running command: $@"
    # shellcheck disable=SC2068
    $@
}

function buildDetectImage() {
    local IMAGE_NAME=$1
    local DOCKERFILE=$2

    removeImage "${IMAGE_NAME}"
    removeImage "${DOCKER_REGISTRY_SIG}/${IMAGE_NAME}"

    logAndRun docker build \
        --build-arg "ALPINE_VERSION=${ALPINE_VERSION}" \
        --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
        --build-arg "DETECT_VERSION=${detectVersion}" \
        -t ${IMAGE_NAME} \
        -f ${DOCKERFILE} \
        .

    publishImage "${IMAGE_NAME}"
}

function publishImage() {
    # Stop execution on an error
    set -e

    local RAW_IMAGE_NAME=$1
    #local INTERNAL_IMAGE_NAME="${DOCKER_REGISTRY_SIG}/${RAW_IMAGE_NAME}"

    # Login information comes from Jenkins OR from the build server run environment

    # Publish internal
    docker tag "${RAW_IMAGE_NAME}" "${INTERNAL_IMAGE_NAME}"
    pushImage "${INTERNAL_IMAGE_NAME}" "${ARTIFACTORY_DEPLOYER_USER}" "${ARTIFACTORY_DEPLOYER_PASSWORD}" "https://${DOCKER_REGISTRY_SIG}/v2/"
    removeImage "${INTERNAL_IMAGE_NAME}"

    # Publish external
    if [[ ${RELEASE_BUILD} == "TRUE" ]]; then
        pushImage "${RAW_IMAGE_NAME}" "${DOCKER_INT_BLACKDUCK_USER}" "${DOCKER_INT_BLACKDUCK_PASSWORD}" "https://index.docker.io/v1/"
        echo ""
    fi

    set +e
}

function pushImage() {
    local IMAGE_NAME=$1
    local DOCKER_LOGIN=$2
    local DOCKER_PASSWORD=$3
    local DOCKER_REGISTRY=$4

    docker login --username "${DOCKER_LOGIN}" --password "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
    docker push "${IMAGE_NAME}"
    docker logout
    echo "Image ${IMAGE_NAME} successfully published"
}

### Build and Push Images

for detectVersion in "${DETECT_VERSIONS[@]}";
    do
    # Build Standard Detect Image
    DETECT_IMAGE_NAME=${ORG}/detect:${detectVersion}
    buildDetectImage ${DETECT_IMAGE_NAME} ${DETECT_DOCKERFILE}

    # TODO- tag latest (major version locked)

    # Build
    DETECT_LITE_IMAGE_NAME=${ORG}/detect:${detectVersion}-lite
    buildDetectImage ${DETECT_LITE_IMAGE_NAME} ${DETECT_LITE_DOCKERFILE}

    # TODO- tag latest (major version locked)

done
