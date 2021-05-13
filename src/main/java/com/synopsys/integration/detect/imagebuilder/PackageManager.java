/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

import java.util.List;

import com.synopsys.integration.detect.imagebuilder.download.Downloader;

public class PackageManager {
    private String name;
    private List<String> versions;
    private String versionCmd;
    private String dockerfileName;
    private Downloader downloader;
    private List<String> detectVersions;
    private List<String> javaVersions;

    public PackageManager(String name, List<String> versions, String versionCmd, String dockerfileName, Downloader downloader, List<String> detectVersions, List<String> javaVersions) {
        this.name = name;
        this.versions = versions;
        this.versionCmd = versionCmd;
        this.dockerfileName = dockerfileName;
        this.downloader = downloader;
        this.detectVersions = detectVersions;
        this.javaVersions = javaVersions;
    }

    public String getName() {
        return name;
    }

    public List<String> getVersions() {
        return versions;
    }

    public String getVersionCmd() {
        return versionCmd;
    }

    public String getDockerfileName() {
        return dockerfileName;
    }

    public boolean hasDownloader() {
        return downloader != null;
    }

    public void downloadFiles(String version, String destination, boolean throwExceptionOnFailedDownload) throws Exception {
        downloader.downloadFiles(version, destination, throwExceptionOnFailedDownload);
    }

    public List<String> getDetectVersions() {
        return detectVersions;
    }

    public List<String> getJavaVersions() {
        return javaVersions;
    }
}