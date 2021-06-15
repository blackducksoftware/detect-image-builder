#!/bin/bash

### Config

# If a variable is unset, stop and exit
set -u

# cd to same directory that script is in
cd "$(dirname "${BASH_SOURCE[0]}")"

RELEASE_BUILD=${DETECT_IMAGE_RELEASE_BUILD:-FALSE}

RUN_DETECT_SCRIPT_NAME=${RUN_DETECT_SCRIPT_NAME:-run-detect.sh}

ORG=blackducksoftware

DETECT_BASE_IMAGE_DOCKERFILE=detect-base-dockerfile

## Versions to support

# This constant will serve as default when accessing a "latest compatible version map" (it should be higher than the highest version of any supported package manager, Detect, etc
readonly NO_LATEST_COMPATIBLE_VERSION=9999

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

    docker images -q "${IMAGE_NAME}" | xargs --verbose --no-run-if-empty docker rmi -f
}

function logAndRun() {
  # shellcheck disable=SC2145
  echo "[$(date)] Running command: $@"
  # shellcheck disable=SC2068
  $@
}

function pushImage() {
    # Stop execution on an error
    set -e

    local IMAGE_NAME=$1
    # Login info is sourced from the build environment
    docker login --username ${DOCKER_INT_BLACKDUCK_USER} --password ${DOCKER_INT_BLACKDUCK_PASSWORD}
    docker push ${IMAGE_NAME}
    docker logout
    echo "Image ${IMAGE_NAME} successfully published"

    set +e
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

<< 'NEW_HIERARCHY'
    alpine:3.13+java11 <-- do we want to have different java's be another level?
           |            |
        pkgmgr      detect-slim
           |
           detect-customer


NEW_HIERARCHY

for detectVersion in "${DETECT_VERSIONS[@]}";
    do
    # Build Detect Base Image
    addSnapshotToImageNameIfNotRelease ${ORG}/detect:${detectVersion}
    removeImage ${IMAGE_NAME}
    logAndRun docker build \
        --build-arg "DETECT_VERSION=${detectVersion}" \
        -t ${IMAGE_NAME} \
        -f ${DETECT_BASE_IMAGE_DOCKERFILE} \
        .

    pushImage "${IMAGE_NAME}"

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
            if [[ ! ${gradleVersion} > ${DETECT_LATEST_COMPATIBLE_GRADLE[${detectVersion}]-${NO_LATEST_COMPATIBLE_VERSION}} ]];
            then
                addSnapshotToImageNameIfNotRelease ${IMAGE_ORG}/detect:${DETECT_VERSION}-gradle-${gradleVersion}
                buildPkgMgrImage ${IMAGE_NAME} ${IMAGE_ORG} ${DETECT_VERSION} ${GRADLE_DOCKERFILE} ${gradleVersion}
                pushImage ${IMAGE_NAME}
            else
                echo "${gradleVersion} is > ${DETECT_LATEST_COMPATIBLE_GRADLE[${detectVersion}]-${NO_LATEST_COMPATIBLE_VERSION}}?"
            fi
    done

    # Maven
    MAVEN_DOCKERFILE=maven-dockerfile
    for mavenVersion in "${MAVEN_VERSIONS[@]}";
        do
            addSnapshotToImageNameIfNotRelease ${ORG}/detect:${DETECT_VERSION}-maven-${mavenVersion}
            buildPkgMgrImage ${IMAGE_NAME} ${ORG} ${DETECT_VERSION} ${MAVEN_DOCKERFILE} ${mavenVersion}
            pushImage ${IMAGE_NAME}
    done

    # Npm
    NPM_DOCKERFILE=npm-dockerfile
    for nodeVersion in "${NODE_VERSIONS[@]}";
        do
            NPM_VERSION=${NODE_TO_NPM_VERSIONS[${nodeVersion}]}
            addSnapshotToImageNameIfNotRelease ${ORG}/detect:${DETECT_VERSION}-npm-${NPM_VERSION}

            # Requires custom build args for npm, node versions
            removeImage "${IMAGE_NAME}"

            logAndRun docker build \
                --build-arg "ORG=${ORG}" \
                --build-arg "DETECT_VERSION=${DETECT_VERSION}" \
                --build-arg "NODE_VERSION=${nodeVersion}" \
                --build-arg "NPM_VERSION=${NPM_VERSION}" \
                -t ${IMAGE_NAME} \
                -f ${NPM_DOCKERFILE} \
                .

            pushImage ${IMAGE_NAME}
    done

done
