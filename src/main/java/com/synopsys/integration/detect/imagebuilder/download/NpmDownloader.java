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

// As of 5/11/21, installation of npm will be handled by apk.
@Deprecated
public class NpmDownloader extends Downloader {
    // Downloading npm by version is very tricky.  We will download Node by version, and use the bundled npm that comes with it.
    String scriptName = "download-npm.sh";
    String nodeBinaryArchitecture = "x64"; //TODO- can we hard-code what architecture this will be built in?
    @Override
    public void downloadFiles(final String version, final String npmFilesDir, boolean throwExceptionOnFailedDownload) throws DownloadFailedException {
        Map<String, String> scriptArgs = new HashMap<>();
        scriptArgs.put("-d", npmFilesDir);
        scriptArgs.put("-v", version);
        scriptArgs.put("-a", nodeBinaryArchitecture);

        downloadUtils.downloadPkgMgrFiles("node", version, npmFilesDir, throwExceptionOnFailedDownload, scriptName, scriptArgs);
    }
}