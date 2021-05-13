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

# Delete any present script
rm -f ${scriptName}

# Get script
curl -o ${scriptName} ${scriptDownloadUrl}

if [[ ! -f ${scriptName} ]]; then
    echo "Could not find ${scriptName}."
fi

if [[ ${version} != "latest" ]] && [[ ${version} != "LATEST" ]]; then
     export DETECT_LATEST_RELEASE_VERSION=${version}
fi

export DETECT_DOWNLOAD_ONLY=1

export DETECT_JAR_DOWNLOAD_DIR=${detectFilesDir}

# Run script to download Detect jar
bash ${scriptName}

# Cleanup script, last-downloaded.txt file
rm -f ${scriptName}
rm -f ${lastDownloadedJarFile}