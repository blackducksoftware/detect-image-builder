#!/usr/bin/sh

# Get args
while getopts d:v:u: flag
do
    case "${flag}" in
        d) detectFilesDir=${OPTARG};;
        v) version=${OPTARG};;
        u) scriptDownloadUrl=${OPTARG};;
    esac
done

scriptName="detect.sh"
lastDownloadedJarFileName="synopsys-detect-last-downloaded-jar.txt"
lastDownloadedJarFile=${detectFilesDir}/${lastDownloadedJarFileName}
useCachedScriptFileName="use-cached-script.txt"

cd ${detectFilesDir}

# If there is no script present, or if "use cached script" file is not present, delete any present script and download new script
if [[ ! -f ${scriptName} ]] || [[ ! -f ${useCachedScriptFileName} ]]; then
    rm -f ${scriptName} \
    && curl -o ${scriptName} ${scriptDownloadUrl}
fi

if [[ ! -f ${scriptName} ]]; then
    echo "Could not find ${scriptName}."
    exit -1
fi

# Tell future runs to use cached script
touch ${useCachedScriptFileName}

if [[ ${version} != "latest" ]] && [[ ${version} != "LATEST" ]]; then
     export DETECT_LATEST_RELEASE_VERSION=${version}
fi

export DETECT_DOWNLOAD_ONLY=1

export DETECT_JAR_DOWNLOAD_DIR=.

# Run script to download Detect jar
bash ${scriptName}