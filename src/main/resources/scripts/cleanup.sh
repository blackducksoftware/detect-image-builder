#!/bin/bash

while getopts d:p:n:j: flag
do
    case "${flag}" in
        p) pkgMgrFilesPath=${OPTARG};;
        d) detectFilesPath=${OPTARG};;
        j) detectJarName=${OPTARG};;
        n) pkgFilesDirName=${OPTARG};;
    esac
done

# Delete jar from DETECT_FILES
cd ${detectFilesPath}

rm -f ${detectJarName}

# Delete everything in PKG_MGR_FILES
cd {pkgMgrFilesPath}/..

rm -rf ${pkgFilesDirName} && mkdir ${pkgFilesDirName}
