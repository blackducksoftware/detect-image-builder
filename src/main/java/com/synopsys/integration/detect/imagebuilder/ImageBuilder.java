/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.synopsys.integration.detect.imagebuilder.download.DetectDownloader;
import com.synopsys.integration.detect.imagebuilder.utilities.EnvUtils;
import com.synopsys.integration.detect.imagebuilder.utilities.RunUtils;

public class ImageBuilder {
    private static EnvUtils envUtils = new EnvUtils();
    private static RunUtils runUtils = new RunUtils();

    private static String BUILD_IMAGES_SCRIPT_NAME = "build-images.sh";
    private static String IMAGE_REPO = "blackducksoftware";
    private static String RESOURCE_FILES_PATH = "src/main/resources"; //TODO- can we get this programatically?
    private static String DOCKERFILE_PATH = RESOURCE_FILES_PATH;
    private static String DETECT_FILES_DIR_NAME = "DETECT_FILES";
    private static String DETECT_FILES_PATH = envUtils.getEnv("DETECT_FILES_PATH", String.format("%s/DETECT_FILES", RESOURCE_FILES_PATH));
    private static String PKG_MGR_FILES_DIR_NAME = "PKG_MGR_FILES";
    private static String PKG_MGR_FILES_PATH = String.format("%s/%s", RESOURCE_FILES_PATH, PKG_MGR_FILES_DIR_NAME);

    public static void main(String[] args) throws Exception {
        Supported supported = new Supported();
        for (PackageManager pkgMgr : supported.getSupported()) {
            for (String pkgMgrVersion : pkgMgr.getVersions()) {
                for (String detectVersion : pkgMgr.getDetectVersions()) {
                    // Download Detect
                    downloadDetect(detectVersion, DETECT_FILES_PATH);
                    String downLoadedDetectJarName;
                    if (detectVersion.equalsIgnoreCase("LATEST")) {
                        downLoadedDetectJarName = getNameOfDownloadedDetectJar(DETECT_FILES_PATH);
                    } else {
                        downLoadedDetectJarName = String.format("synopsys-detect-%s.jar", detectVersion);
                    }

                    for (String javaVersion : pkgMgr.getJavaVersions()) {
                        String pkgMgrName = pkgMgr.getName();
                        String imageTag = determineImageTag(detectVersion, pkgMgrName, pkgMgrVersion, javaVersion);
                        String imageName = String.format("%s/%s", IMAGE_REPO, imageTag);
                        String downloadDestination = String.format("%s/%s", PKG_MGR_FILES_PATH, pkgMgrName);

                        // Download Package Manager files
                        if (pkgMgr.hasDownloader()) {
                            pkgMgr.downloadFiles(pkgMgrVersion, downloadDestination, false); //TODO- should we throw exception on failed download of pkg mgr?
                        }

                        // Build image
                        Map<String, String> scriptArgs = new HashMap<>();
                        scriptArgs.put("-d", DOCKERFILE_PATH);
                        scriptArgs.put("-f", pkgMgr.getDockerfileName());
                        scriptArgs.put("-p", String.format("%s/%s/%s/%s", PKG_MGR_FILES_DIR_NAME, pkgMgrName, pkgMgrVersion, pkgMgrName));
                        scriptArgs.put("-n", pkgMgrName);
                        scriptArgs.put("-v", pkgMgr.getVersionCmd());
                        scriptArgs.put("-i", imageName);
                        scriptArgs.put("-e", DETECT_FILES_DIR_NAME);
                        scriptArgs.put("-o", downLoadedDetectJarName);
                        scriptArgs.put("-j", javaVersion);

                        runUtils.runScript(BUILD_IMAGES_SCRIPT_NAME, scriptArgs);

                        // TODO- Push image to internal artifactory
                        // TODO- what about signing images (only an issue externally)?
                        //      publish to public-facing Artifactory as well as Docker Hub --> collab w/ releng team to handle builds that sign/deploy built images
                    }
                }
            }
        }

    }

    private static void downloadDetect(String detectVersion, String detectFilesPath) throws DownloadFailedException {
        DetectDownloader detectDownloader = new DetectDownloader();
        detectDownloader.downloadFiles(detectVersion, detectFilesPath, true);
    }

    private static String getNameOfDownloadedDetectJar(String detectJarPath) throws Exception {
        Pattern jarNamePattern = Pattern.compile("synopsys-detect-.*\\.jar");
        File detectJarDir = new File(detectJarPath);
        for (File file : detectJarDir.listFiles()) {
            if (jarNamePattern.matcher(file.getName()).matches()) {
                return file.getName();
            }
        }
        throw new Exception("Cannot parse name of downloaded Detect jar.");
    }

    private static String determineImageTag(String detectVersion, String pkgMgrName, String pkgMgrVersion, String javaVersion) {
        String prefix;
        if (!detectVersion.equalsIgnoreCase("LATEST")) {
            prefix = String.format("detect:%s-", detectVersion);
        } else {
            prefix = "detect:";
        }

        if (pkgMgrVersion.equals("")) {
            // If package manager is imported by a linux package manager at build-time that only supports one version of the package manager, omit pkgMgr version from tag
            return String.format("%s%s-java-%s", prefix, pkgMgrName, javaVersion);
        } else {
            return String.format("%s%s-%s-java-%s", prefix, pkgMgrName, pkgMgrVersion, javaVersion);
        }
    }
}
