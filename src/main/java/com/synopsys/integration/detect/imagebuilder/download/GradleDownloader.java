/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.download;

import java.util.HashMap;
import java.util.Map;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;

public class GradleDownloader extends Downloader {
    private String gradleDownloadScriptName = "download-gradle.sh";

    @Override
    public void downloadFiles(String version, String gradleFilesDir, boolean throwExceptionOnFailedDownload) throws DownloadFailedException {
        Map<String, String> scriptArgs = new HashMap<>();
        scriptArgs.put("-v", version);
        scriptArgs.put("-d", gradleFilesDir);
        downloadUtils.downloadPkgMgrFiles("gradle", version, gradleFilesDir, throwExceptionOnFailedDownload, gradleDownloadScriptName, scriptArgs);
    }
}