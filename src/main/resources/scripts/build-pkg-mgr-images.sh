#!/usr/bin/sh

# Get args
while getopts d:p:n:v:i:e:j:f:o: flag
do
    case "${flag}" in
        d) dockerfilePath=${OPTARG};;
        p) pkgMgrFilesPath=${OPTARG};;
        n) pkgMgrName=${OPTARG};;
        v) pkgMgrVersionCmd=${OPTARG};;
        i) imageName=${OPTARG};;
        e) detectFilesPath=${OPTARG};;
        j) javaVersion=${OPTARG};;
        f) dockerfileName=${OPTARG};;
        o) detectJarName=${OPTARG};;
    esac
done

# Delete image if already present

cd ${dockerfilePath} && \
    docker rmi -f ${imageName} && \
    docker build \
    --build-arg "DETECT_FILES_DIR=${detectFilesPath}" \
    --build-arg "DETECT_JAR_NAME=${detectJarName}" \
    --build-arg "JAVA_VERSION=${javaVersion}" \
    -t ${imageName} \
    -f ${dockerfileName} \
    .
