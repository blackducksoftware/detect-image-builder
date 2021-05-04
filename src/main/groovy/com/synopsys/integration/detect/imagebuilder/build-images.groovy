package com.synopsys.integration.detect.imagebuilder

import com.synopsys.integration.detect.imagebuilder.PackageManagers

// This script downloads Detect and supported package manager files, builds images, pushes them to registry

def buildImages() {
    def env = System.getenv()

    def imageRepo = 'blackducksoftware'
    def detectVersion = env['DETECT_VERSION']
    def dockerfilePath = null != env['DOCKERFILE_PATH'] ? env['DOCKERFILE_PATH'] : "./"

    // Download Detect


    // Download package manager files
    try {
        PackageManagers packageManagers = new PackageManagers()
        // set Package manager dir env var
        for (PackageManager pkgMgr in packageManagers.getSupported()) {
            def imageNamePrefix
            if (null != detectVersion && !detectVersion.equals("LATEST")) {
                imageNamePrefix = "detect-${detectVersion}"
            } else {
                imageNamePrefix = "detect"
            }

            def pkgMgrName = pkgMgr.name
            def imageName = "${imageRepo}/${imageNamePrefix}-${pkgMgrName}-${pkgMgr.version}"
            def detectPathProperty = pkgMgr.detectPathProperty

            pkgMgr.downloadFiles()

            sh """
               cd ${dockerfilePath}
               docker build \
               --build-arg “PKG_MGR_DIR=PKG_MGR_FILES/${pkgMgrName}” \
               --build-arg "PKG_MGR_PATH_PROPERTY_KEY=${detectPathProperty} \
               -t ${imageName} \
               .
           """

            // Push image
        }
    } catch (Exception e) {

    }
}
