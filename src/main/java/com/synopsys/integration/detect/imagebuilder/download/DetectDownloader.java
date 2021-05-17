/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.download;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;

public class DetectDownloader extends Downloader {
    private String scriptName = "download-detect.sh";
    private String detectScriptDownloadUrl = envUtils.getEnv("DETECT_SCRIPT_DOWNLOAD_URL", "https://detect.synopsys.com/detect.sh");
    private Pattern jarNamePattern = Pattern.compile("synopsys-detect-.*\\.jar");

    @Override
    public void downloadFiles(String version, String detectFilesDirPath, boolean throwExceptionOnFailedDownload, String scriptsPath) throws DownloadFailedException {
        Map<String, String> scriptAgrs = new HashMap<>();
        scriptAgrs.put("-v", version);
        scriptAgrs.put("-u", detectScriptDownloadUrl);
        scriptAgrs.put("-d", detectFilesDirPath);

        runUtils.runScript(String.format("%s/%s", scriptsPath, scriptName), scriptAgrs);

        // Look for jar
        File detectFilesDir = new File(detectFilesDirPath);
        boolean foundJar = false;
        if (detectFilesDir.isDirectory() && detectFilesDir.listFiles() != null) {
            foundJar = Arrays.asList(detectFilesDir.listFiles()).stream()
                                   .anyMatch(file -> jarNamePattern.matcher(file.getName()).matches());
        }
        if (!foundJar) {
            if (throwExceptionOnFailedDownload) {
                throw new DownloadFailedException("Detect jar.");
            }
            logger.warn("Failed to download Detect jar.");
        }
        logger.debug(String.format("Successfully downloaded Detect jar to %s.", detectFilesDirPath));
    }

}
