#!/bin/bash

### Config

# If a variable is unset, stop and exit
set -u

# cd to same directory that script is in
cd "$(dirname "${BASH_SOURCE[0]}")"

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}

IMAGE_ORG=blackducksoftware

DETECT_BASE_IMAGE_DOCKERFILE=detect-base-dockerfile

# Versions to support
DETECT_VERSIONS=( 7.0.0 6.9.1 )

GRADLE_VERSIONS=( 6.8.2 )

MAVEN_VERSIONS=( 3.8.1 )

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
#   If a package manager doesn't require one of the args (ex. npm doesn't specify a PKG_MGR_VERSION since we're only using the version alpine supports) provide an empty string in its place
function buildPkgMgrImage() {
    local IMAGE_NAME=$1
    local ORG=$2
    local DETECT_VERSION=$3
    local DOCKERFILE_NAME=$4
    local PKG_MGR_VERSION=${5:-unused}

    removeImage "${IMAGE_NAME}"
    removeImage "${DOCKER_REGISTRY_SIG}/${IMAGE_NAME}"

    logAndRun docker build \
        --build-arg "ORG=${ORG}" \
        --build-arg "DETECT_VERSION=${DETECT_VERSION}" \
        --build-arg "PKG_MGR_VERSION=${PKG_MGR_VERSION}" \
        -t ${IMAGE_NAME} \
        -f ${DOCKERFILE_NAME} \
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
    local INTERNAL_IMAGE_NAME="${DOCKER_REGISTRY_SIG}/${RAW_IMAGE_NAME}"

    # Login information comes from Jenkins OR from the build server run environment

    # Publish internal
    docker tag "${RAW_IMAGE_NAME}" "${INTERNAL_IMAGE_NAME}"
    pushImage "${INTERNAL_IMAGE_NAME}" "${ARTIFACTORY_DEPLOYER_USER}" "${ARTIFACTORY_DEPLOYER_PASSWORD}" "https://${DOCKER_REGISTRY_SIG}/v2/"
    removeImage "${INTERNAL_IMAGE_NAME}"

    # Publish external
    if [[ ${RELEASE_BUILD} == "TRUE" ]]; then
        pushImage "${RAW_IMAGE_NAME}" "${DOCKER_INT_BLACKDUCK_USER}" "${DOCKER_INT_BLACKDUCK_PASSWORD}" "https://index.docker.io/v1/"
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

# This function will set global variable IMAGE_NAME
function addSnapshotToImageNameIfNotRelease() {
    local ORIGINAL_IMAGE_NAME=$1

    if [[ ${RELEASE_BUILD} == "TRUE" ]]; then
        IMAGE_NAME=${ORIGINAL_IMAGE_NAME}
    else
        IMAGE_NAME=${ORIGINAL_IMAGE_NAME}-SNAPSHOT
    fi
}

### Build and Push Images

for detectVersion in "${DETECT_VERSIONS[@]}";
    do
    # Build Detect Base Image
    addSnapshotToImageNameIfNotRelease ${IMAGE_ORG}/detect:${detectVersion}

    removeImage "${IMAGE_NAME}"
    removeImage "${DOCKER_REGISTRY_SIG}/${IMAGE_NAME}"

    logAndRun docker build \
        --build-arg "DETECT_VERSION=${detectVersion}" \
        -t ${IMAGE_NAME} \
        -f ${DETECT_BASE_IMAGE_DOCKERFILE} \
        .

    publishImage "${IMAGE_NAME}"

    # Build Package Manager Images

    if [[ ${RELEASE_BUILD} == "TRUE" ]]; then
        DETECT_VERSION=${detectVersion}
    else
        DETECT_VERSION=${detectVersion}-SNAPSHOT
    fi
    # Now DETECT_VERSION is most updated version

    # Gradle
    GRADLE_DOCKERFILE=gradle-dockerfile
    for gradleVersion in "${GRADLE_VERSIONS[@]}";
        do
            addSnapshotToImageNameIfNotRelease ${IMAGE_ORG}/detect:${DETECT_VERSION}-gradle-${gradleVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${DETECT_VERSION} ${GRADLE_DOCKERFILE} ${gradleVersion}
            publishImage ${IMAGE_NAME}
    done

    # Maven
    MAVEN_DOCKERFILE=maven-dockerfile
    for mavenVersion in "${MAVEN_VERSIONS[@]}";
        do
            addSnapshotToImageNameIfNotRelease ${IMAGE_ORG}/detect:${DETECT_VERSION}-maven-${mavenVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${DETECT_VERSION} ${MAVEN_DOCKERFILE} ${mavenVersion}
            publishImage ${IMAGE_NAME}
    done

    # Npm
    NPM_DOCKERFILE=npm-dockerfile
    addSnapshotToImageNameIfNotRelease ${IMAGE_ORG}/detect:${DETECT_VERSION}-npm
    buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${DETECT_VERSION} ${NPM_DOCKERFILE}
    publishImage ${IMAGE_NAME}

done
