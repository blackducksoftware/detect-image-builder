#!/bin/bash

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

# Delete jar from DETECT_FILES, everything in PKG_MGR_FILES