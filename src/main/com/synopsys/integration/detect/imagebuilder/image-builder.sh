#!/usr/local/bin/bash

### Config

# If a variable is unset, stop and exit
set -u

# cd to same directory that script is in
cd "$(dirname "${BASH_SOURCE[0]}")"

RELEASE_BUILD=${RUN_RELEASE:-FALSE}

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}

ORG=blackducksoftware

DETECT_BASE_IMAGE_DOCKERFILE=detect-base-dockerfile

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
    removeImage "$(getInternalImageName "${IMAGE_NAME}")"

    logAndRun docker build \
        --build-arg "ORG=${ORG}" \
        --build-arg "DETECT_VERSION=${detectVersion}" \
        --build-arg "PKG_MGR_VERSION=${PKG_MGR_VERSION}" \
        -t ${IMAGE_NAME} \
        -f ${DOCKERFILE_NAME} \
        .
}

function getInternalImageName() {
    echo "${DOCKER_REGISTRY_SIG}/$1"
}

function removeImage() {
    local IMAGE_NAME=$1

    test -n "$(docker images -q "${IMAGE_NAME}")" && docker rmi -f "${IMAGE_NAME}"
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
    local INTERNAL_IMAGE_NAME="$(getInternalImageName "${RAW_IMAGE_NAME}")"

    # Login information comes from Jenkins OR from the build server run environment

    # Publish internal
    docker tag "${RAW_IMAGE_NAME}" "${INTERNAL_IMAGE_NAME}"
    #pushImage "${INTERNAL_IMAGE_NAME}" "${ARTIFACTORY_DEPLOYER_USER}" "${ARTIFACTORY_DEPLOYER_PASSWORD}" "https://${DOCKER_REGISTRY_SIG}/v2/"
    #removeImage "${INTERNAL_IMAGE_NAME}"

    if [[ ${RELEASE_BUILD} == "TRUE" ]];
    then
        # Publish external
        #pushImage "${RAW_IMAGE_NAME}" "${DOCKER_INT_BLACKDUCK_USER}" "${DOCKER_INT_BLACKDUCK_PASSWORD}" "https://index.docker.io/v1/"
        echo "fake publish"
    fi
    set +e
}

function pushImage() {
    local IMAGE_NAME=$1
    local DOCKER_LOGIN=$2
    local DOCKER_PASSWORD=$3
    local DOCKER_REGISTRY=$4

    #docker login --username "${DOCKER_LOGIN}" --password "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
    #docker push "${IMAGE_NAME}"
    #docker logout
    echo "Image ${IMAGE_NAME} successfully published"
}

### Build and Push Images

for detectVersion in "${DETECT_VERSIONS[@]}";
    do
    # Build Detect Base Image
    IMAGE_NAME=${ORG}/detect:${detectVersion}
    removeImage "${IMAGE_NAME}"
    removeImage "$(getInternalImageName "${IMAGE_NAME}")"

    logAndRun docker build \
        --build-arg "ALPINE_VERSION=${ALPINE_VERSION}" \
        --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
        --build-arg "DETECT_VERSION=${detectVersion}" \
        -t ${IMAGE_NAME} \
        -f ${DETECT_BASE_IMAGE_DOCKERFILE} \
        .

    #publishImage "${IMAGE_NAME}"

    # Build Package Manager Images

    # Gradle
    GRADLE_DOCKERFILE=gradle-dockerfile
    for gradleVersion in "${GRADLE_VERSIONS[@]}";
        do
            if [[ ! ${gradleVersion} > ${DETECT_LATEST_COMPATIBLE_GRADLE[${detectVersion}]-${NO_LATEST_COMPATIBLE_VERSION}} ]];
            then
                IMAGE_NAME=${ORG}/detect:${detectVersion}-gradle-${gradleVersion}
                buildPkgMgrImage ${IMAGE_NAME} ${ORG} ${detectVersion} ${GRADLE_DOCKERFILE} ${gradleVersion}
                publishImage ${IMAGE_NAME}
            fi
    done

    # Maven
    MAVEN_DOCKERFILE=maven-dockerfile
    for mavenVersion in "${MAVEN_VERSIONS[@]}";
        do
            IMAGE_NAME=${ORG}/detect:${detectVersion}-maven-${mavenVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${ORG} ${detectVersion} ${MAVEN_DOCKERFILE} ${mavenVersion}
            publishImage ${IMAGE_NAME}
    done

    # Npm
    NPM_DOCKERFILE=npm-dockerfile
    for nodeVersion in "${NODE_VERSIONS[@]}";
        do
            NPM_VERSION=${NODE_TO_NPM_VERSIONS[${nodeVersion}]}
            IMAGE_NAME=${ORG}/detect:${detectVersion}-npm-${NPM_VERSION}

            # Requires custom build args for npm, node versions
            removeImage "${IMAGE_NAME}"

            logAndRun docker build \
                --build-arg "ORG=${ORG}" \
                --build-arg "ALPINE_VERSION=${ALPINE_VERSION}" \
                --build-arg "DETECT_VERSION=${detectVersion}" \
                --build-arg "NODE_VERSION=${nodeVersion}" \
                --build-arg "NPM_VERSION=${NPM_VERSION}" \
                -t ${IMAGE_NAME} \
                -f ${NPM_DOCKERFILE} \
                .

            publishImage ${IMAGE_NAME}
    done

done
