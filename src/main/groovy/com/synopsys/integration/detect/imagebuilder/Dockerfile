FROM openjdk:8-jdk-slim

ARG SHARED_FILES_DIR='SHARED_FILES'
ARG PKG_MGR_DIR
ARG PKG_MGR_PATH_PROPERTY_KEY

# Copy in shared files
COPY ./${SHARED_FILES_DIR} /${SHARED_FILES_DIR}/

# Copy in Package Manager files
COPY ./${PKG_MGR_DIR} /${PKG_MGR_DIR}/

# Store Detect property key=value for path to package manager
ENV PKG_MGR_PATH_PROPERTY="${PKG_MGR_PATH_PROPERTY_KEY}=${PKG_MGR_DIR}"

# Define Docker Image entrypoint
ENTRYPOINT ["${SHARED_FILES_DIR}/run-detect.sh"]