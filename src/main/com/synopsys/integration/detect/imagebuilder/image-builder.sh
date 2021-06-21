#!/usr/local/bin/bash

### Config

# If a variable is unset, stop and exit
set -u

# cd to same directory that script is in
cd "$(dirname "${BASH_SOURCE[0]}")"

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}

ORG=blackducksoftware

INTEGRATIONS_BASE_IMAGE_NAME=${ORG}/integrations-base
INTEGRATIONS_BASE_IMAGE_DOCKERFILE=integrations-base-dockerfile

DETECT_DOCKERFILE=detect-dockerfile

## Versions to support

# This constant will serve as default when accessing a "latest compatible version map" (it should be higher than the highest version of any supported package manager, Detect, etc
readonly NO_LATEST_COMPATIBLE_VERSION=9999

# Alpine
ALPINE_VERSION=3.13

# Java
JAVA_VERSION=11

# Detect
DETECT_VERSIONS=( 7.0.0 6.9.1 )

# Gradle
GRADLE_VERSIONS=( 6.8.2 6.7.1 )

declare -A DETECT_LATEST_COMPATIBLE_GRADLE
DETECT_LATEST_COMPATIBLE_GRADLE[6.9.1]=6.7.1

# Maven
MAVEN_VERSIONS=( 3.8.1 )

# Npm
NODE_VERSIONS=( 14.16.1-r1 ) # Need to specify npm by node version when getting from Alpine apk, so we will maintain a map of node versions to npm versions
declare -A NODE_TO_NPM_VERSIONS
NODE_TO_NPM_VERSIONS[14.16.1-r1]=6.14.12

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
DOCKER_REGISTRY_SIG="${DOCKER_REGISTRY_SIG}"
ARTIFACTORY_DEPLOYER_USER="${ARTIFACTORY_DEPLOYER_USER}"
ARTIFACTORY_DEPLOYER_PASSWORD="${ARTIFACTORY_DEPLOYER_PASSWORD}"
DOCKER_INT_BLACKDUCK_USER="${DOCKER_INT_BLACKDUCK_USER}"
DOCKER_INT_BLACKDUCK_PASSWORD="${DOCKER_INT_BLACKDUCK_PASSWORD}"

### Functions

# NOTE: When supplying arguments to this function, ORDER MATTERS.
#   If a package manager doesn't require one of the args provide an empty string in its place
function buildPkgMgrImage() {
    local IMAGE_NAME=$1
    local DOCKERFILE_NAME=$2
    local PKG_MGR_VERSION=${3:-unused}

    removeImage "${IMAGE_NAME}"
    removeImage "${DOCKER_REGISTRY_SIG}/${IMAGE_NAME}"

    logAndRun docker build \
        --build-arg "ORG=${ORG}" \
        --build-arg "PKG_MGR_VERSION=${PKG_MGR_VERSION}" \
        -t ${IMAGE_NAME} \
        -f ${DOCKERFILE_NAME} \
        .
}

function buildDetectImage() {
    local IMAGE_NAME=$1
    local BASE_IMAGE=$2
    local DETECT_VERSION=$3

    removeImage "${IMAGE_NAME}"
    removeImage "${DOCKER_REGISTRY_SIG}/${IMAGE_NAME}"

    logAndRun docker build \
        --build-arg "BASE_IMAGE=${BASE_IMAGE}" \
        --build-arg "DETECT_VERSION=${DETECT_VERSION}" \
        -t ${IMAGE_NAME} \
        -f ${DETECT_DOCKERFILE} \
        .
}

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

function publishImage() {
    # Stop execution on an error
    set -e

    local RAW_IMAGE_NAME=$1

    publishImageInternal ${RAW_IMAGE_NAME}
    # Login information comes from Jenkins OR from the build server run environment

    # Publish external
    if [[ ${RELEASE_BUILD} == "TRUE" ]]; then
        pushImage "${RAW_IMAGE_NAME}" "${DOCKER_INT_BLACKDUCK_USER}" "${DOCKER_INT_BLACKDUCK_PASSWORD}" "https://index.docker.io/v1/"
        echo ""
    fi

    set +e
}

function publishImageInternal() {
    # Stop execution on an error
    set -e

    local RAW_IMAGE_NAME=$1
    local INTERNAL_IMAGE_NAME="${DOCKER_REGISTRY_SIG}/${RAW_IMAGE_NAME}"

    # Login information comes from Jenkins OR from the build server run environment

    # Publish internal
    docker tag "${RAW_IMAGE_NAME}" "${INTERNAL_IMAGE_NAME}"
    pushImage "${INTERNAL_IMAGE_NAME}" "${ARTIFACTORY_DEPLOYER_USER}" "${ARTIFACTORY_DEPLOYER_PASSWORD}" "https://${DOCKER_REGISTRY_SIG}/v2/"
    removeImage "${INTERNAL_IMAGE_NAME}"

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

## Build Base Image
removeImage "${INTEGRATIONS_BASE_IMAGE_NAME}"
removeImage "${DOCKER_REGISTRY_SIG}/${INTEGRATIONS_BASE_IMAGE_NAME}"
logAndRun docker build \
        --build-arg "ALPINE_VERSION=${ALPINE_VERSION}" \
        --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
        -t ${INTEGRATIONS_BASE_IMAGE_NAME} \
        -f ${INTEGRATIONS_BASE_IMAGE_DOCKERFILE} \
        .

publishImageInternal "${INTEGRATIONS_BASE_IMAGE_NAME}"

## Build Detect Slim Images
for detectVersion in "${DETECT_VERSIONS[@]}";
    do
    IMAGE_NAME=${ORG}/detect:${detectVersion}
    buildDetectImage ${IMAGE_NAME} ${INTEGRATIONS_BASE_IMAGE_NAME} ${detectVersion}
    publishImage "${IMAGE_NAME}"
done


## Build Package Manager Images, Customer Detect Images

# Gradle
GRADLE_DOCKERFILE=gradle-dockerfile
for gradleVersion in "${GRADLE_VERSIONS[@]}";
    do
        GRADLE_IMAGE_NAME=${ORG}/gradle:${gradleVersion}
        buildPkgMgrImage ${GRADLE_IMAGE_NAME} ${GRADLE_DOCKERFILE} ${gradleVersion}
        publishImageInternal ${GRADLE_IMAGE_NAME}

        # Detect Gradle
        for detectVersion in "${DETECT_VERSIONS[@]}";
            do
            if [[ ! ${gradleVersion} > ${DETECT_LATEST_COMPATIBLE_GRADLE[${detectVersion}]-${NO_LATEST_COMPATIBLE_VERSION}} ]];
               then
               DETECT_IMAGE_NAME=${ORG}/detect:${detectVersion}-gradle-${gradleVersion}
               buildDetectImage ${DETECT_IMAGE_NAME} ${GRADLE_IMAGE_NAME} ${detectVersion}
               publishImage ${DETECT_IMAGE_NAME}
            fi
        done
done

# Maven
MAVEN_DOCKERFILE=maven-dockerfile
for mavenVersion in "${MAVEN_VERSIONS[@]}";
    do
        MAVEN_IMAGE_NAME=${ORG}/maven:${mavenVersion}
        buildPkgMgrImage ${MAVEN_IMAGE_NAME} ${MAVEN_DOCKERFILE} ${mavenVersion}
        publishImageInternal ${MAVEN_IMAGE_NAME}

        # Detect Maven
        for detectVersion in "${DETECT_VERSIONS[@]}";
            do
               DETECT_IMAGE_NAME=${ORG}/detect:${detectVersion}-maven-${mavenVersion}
               buildDetectImage ${DETECT_IMAGE_NAME} ${MAVEN_IMAGE_NAME} ${detectVersion}
               publishImage ${DETECT_IMAGE_NAME}
        done
done

# Npm
NPM_DOCKERFILE=npm-dockerfile
for nodeVersion in "${NODE_VERSIONS[@]}";
    do
        NPM_VERSION=${NODE_TO_NPM_VERSIONS[${nodeVersion}]}
        NPM_IMAGE_NAME=${ORG}/npm:${NPM_VERSION}

        # Requires custom build args for npm, node versions
        removeImage "${NPM_IMAGE_NAME}"

        logAndRun docker build \
            --build-arg "ORG=${ORG}" \
            --build-arg "ALPINE_VERSION=${ALPINE_VERSION}" \
            --build-arg "DETECT_VERSION=${detectVersion}" \
            --build-arg "NODE_VERSION=${nodeVersion}" \
            --build-arg "NPM_VERSION=${NPM_VERSION}" \
            -t ${NPM_IMAGE_NAME} \
            -f ${NPM_DOCKERFILE} \
            .

        publishImageInternal ${NPM_IMAGE_NAME}

        # Detect Npm
        for detectVersion in "${DETECT_VERSIONS[@]}";
            do
               DETECT_IMAGE_NAME=${ORG}/detect:${detectVersion}-npm-${NPM_VERSION}
               buildDetectImage ${DETECT_IMAGE_NAME} ${NPM_IMAGE_NAME} ${detectVersion}
               publishImage ${DETECT_IMAGE_NAME}
        done
done
