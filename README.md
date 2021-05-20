# Overview

Detect Image Builder builds Docker images configured to run Detect from within a Docker container.

## Configuration

To configure supported package manager versions and Detect versions, edit image-builder.sh.

To enable pushing of images to Docker Hub, set environment variables DOCKER_INT_BLACKDUCK_USER and DOCKER_INT_BLACKDUCK_PASSWORD.

To run:
```bash
    bash image-builder.sh
```

# Documentation

Detect Image Builder is comprised of:
 - A bash script image-builder.sh
 - A bash script run-detect.sh that serves as the entry point for all Detect images
 - A Dockerfile detect-base-dockerfile that is used to build a base Detect image containing just Detect, Java and run-detect.sh
 - Several Dockerfiles that image-builder.sh passes arguments to in order to build Docker images packaged with package manager executables in order to support Detect Detectors

 
 Images are pushed to Docker Hub under the repo blackducksoftware/detect.
    