/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.google.gson.Gson;
import com.synopsys.integration.detect.imagebuilder.utilities.EnvUtils;
import com.synopsys.integration.detect.imagebuilder.utilities.RunUtils;

public class ImageBuilder implements ApplicationRunner {
    Logger logger = LoggerFactory.getLogger(getClass());
    private EnvUtils envUtils = new EnvUtils();
    private RunUtils runUtils = new RunUtils();
    private Gson gson = new Gson();

    private String RESOURCE_FILES_PATH = envUtils.getEnv("RESOURCES_PATH", "src/main/resources");
    private String DOCKERFILES_PATH = envUtils.getEnv("DOCKERFILES_PATH", RESOURCE_FILES_PATH);
    private String DETECT_JAVA_DOCKERFILE_NAME = envUtils.getEnv("DETECT_JAVA_DOCKRFILE_NAME", "detect-java-dockerfile");
    private String DETECT_JAVA_IMAGE_VERSION = envUtils.getEnv("DETECT_JAVA_IMAGE_VERSION", "2.0");
    private String BUILD_DETECT_JAVA_IMAGES_SCRIPT_NAME = "build-detect-java-images.sh";
    private String DETECT_FILES_DIR_NAME = envUtils.getEnv("DETECT_FILES_DIR_NAME", "DETECT_FILES");
    private String DETECT_FILES_PATH = envUtils.getEnv("DETECT_FILES_PATH", String.format("%s/%s", RESOURCE_FILES_PATH, DETECT_FILES_DIR_NAME));
    private String SCRIPTS_DIR_NAME = envUtils.getEnv("SCRIPTS_DIR_NAMW", "scripts");
    private String SCRIPTS_PATH = envUtils.getEnv("SCRIPTS_PATH", String.format("%s/%s", RESOURCE_FILES_PATH, SCRIPTS_DIR_NAME));
    private String BUILD_PKG_MGR_IMAGES_SCRIPT_NAME = "build-pkg-mgr-images.sh";
    //private String IMAGE_ORG = "blackducksoftware";
    private String IMAGE_ORG = "alexcrowley14";
    private String CLEANUP_RESOURCE_FILES = envUtils.getEnv("CLEANUP_RESOURCE_FILES", "TRUE");

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(ImageBuilder.class);
        builder.logStartupInfo(false);
        builder.run(args);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        Supported supported = new Supported();
        // Build + push detect-java images that will serve as base for package manager images
        for (String detectVersion : supported.getSupportedDetectVersions()) {
            // Download Detect
            downloadDetect(detectVersion, DETECT_FILES_PATH, SCRIPTS_PATH);
            String downLoadedDetectJarVersion;
            if (detectVersion.equalsIgnoreCase("LATEST")) {
                downLoadedDetectJarVersion = getVersionOfDownloadedDetectJar(DETECT_FILES_PATH);
            } else {
                downLoadedDetectJarVersion = detectVersion;
            }

            for (String javaVersion : supported.getSupportedJavaVersions()) {
                Map<String, String> buildScriptArgs = new HashMap<>();
                buildScriptArgs.put("-e", DETECT_FILES_PATH);
                buildScriptArgs.put("-i", downLoadedDetectJarVersion);
                buildScriptArgs.put("-d", DOCKERFILES_PATH);
                buildScriptArgs.put("-f", DETECT_JAVA_DOCKERFILE_NAME);
                buildScriptArgs.put("-j", javaVersion);
                buildScriptArgs.put("-v", DETECT_JAVA_IMAGE_VERSION);
                buildScriptArgs.put("-o", IMAGE_ORG);

                runUtils.runScript(String.format("%s/%s", SCRIPTS_PATH, BUILD_DETECT_JAVA_IMAGES_SCRIPT_NAME), buildScriptArgs);
            }

            //TODO- push detect-java image to internal artifactory
            // this needs to take place before
        }
        // Build Package Manager images (based on detect-java images)
        for (PackageManager pkgMgr : supported.getSupportedPackageManagers()) {
            for (String pkgMgrVersion : pkgMgr.getVersions()) {
                for (String detectVersion : pkgMgr.getDetectVersions()) {
                    for (String javaVersion : pkgMgr.getJavaVersions()) {
                        String pkgMgrName = pkgMgr.getName();
                        String imageTag = determineImageTag(detectVersion, pkgMgrName, pkgMgrVersion, javaVersion);
                        String imageName = String.format("%s/%s", IMAGE_ORG, imageTag);

                        //TODO- adapt these to new dockerfile
                        // Build image
                        Map<String, String> buildArgs = new HashMap<>();
                        buildArgs.put("-d", DOCKERFILES_PATH);
                        buildArgs.put("-f", pkgMgr.getDockerfileName());
                        buildArgs.put("-n", pkgMgrName);
                        buildArgs.put("-i", imageName);
                        buildArgs.put("-j", javaVersion);

                        String pathToBuildImagesScript = String.format("%s/%s", SCRIPTS_PATH, BUILD_PKG_MGR_IMAGES_SCRIPT_NAME);
                        runUtils.runScript(pathToBuildImagesScript, buildArgs);

                        // TODO- Push image to internal artifactory
                        // TODO- what about signing images (only an issue externally)?
                        //      publish to public-facing Artifactory as well as Docker Hub --> collab w/ releng team to handle builds that sign/deploy built images
                    }
                }
            }
        }
        // Cleanup downloaded Detect files, package manager files
        if (CLEANUP_RESOURCE_FILES.equalsIgnoreCase("TRUE")) {
            cleanup();
        }

    }

    private void downloadDetect(String detectVersion, String detectFilesPath, String scriptsPath) throws DownloadFailedException {
        String potentialJarName = String.format("synopsys-detect-%s.jar", detectVersion);
        String jarPath = String.format("%s/%s", detectFilesPath, potentialJarName);
        if (new File(jarPath).exists()) {
            logger.info(String.format("Already downloaded %s", potentialJarName));
            return;
        }
        DetectDownloader detectDownloader = new DetectDownloader();
        detectDownloader.downloadFiles(detectVersion, detectFilesPath, true, scriptsPath);
    }

    private String getVersionOfDownloadedDetectJar(String detectJarPath) throws Exception {
        File lastDownloadJarTxtFileDir = new File(detectJarPath);
        String lastDownloadJarTxtFileName = "synopsys-detect-last-downloaded-jar.txt";
        for (File file : lastDownloadJarTxtFileDir.listFiles()) {
            if (file.getName().equals(lastDownloadJarTxtFileName)) {
                // Read file for name of jar
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String jarName = reader.readLine();
                return jarName.replace("synopsys-detect-", "").replace(".jar", "");
            }
        }
        throw new Exception("Cannot parse name of downloaded Detect jar.");
    }

    private String determineImageTag(String detectVersion, String pkgMgrName, String pkgMgrVersion, String javaVersion) {
        String prefix;
        if (!detectVersion.equalsIgnoreCase("LATEST")) { // TODO- should we always include
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

    private void cleanup() {
        // Delete Detect jars
        File detectFilesDir = new File(DETECT_FILES_PATH);
        List<String> detectFilesNotToDelete = Arrays.asList("run-detect.sh");
        if (detectFilesDir.isDirectory() || detectFilesDir.listFiles() != null && detectFilesDir.listFiles().length != 0) {
            for (File file : detectFilesDir.listFiles()) {
                if (!detectFilesNotToDelete.contains(file.getName())) {
                    file.delete();
                }
            }
        }
    }
}
