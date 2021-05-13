#!/bin/sh

# Get args
while getopts d:v:a: flag
do
    case "${flag}" in
        d) npmFilesDir=${OPTARG};;
        v) nodeVersion=${OPTARG};;
        a) architecture=${OPTARG};;
    esac
done

nodeTarName=node.tar.gz
versionDir=${npmFilesDir}/${nodeVersion}

cd ${versionDir} \
    && curl -L https://nodejs.org/dist/${nodeVersion}/node-${nodeVersion}-linux-${architecture}.tar.gz -o ${nodeTarName} \
    && tar -xzf ${nodeTarName} \
    && rm ${nodeTarName} \
    && ln -s node-${nodeVersion}-linux-${architecture} npm \
    && ln -s npm/bin/node npm/bin/nodejs
