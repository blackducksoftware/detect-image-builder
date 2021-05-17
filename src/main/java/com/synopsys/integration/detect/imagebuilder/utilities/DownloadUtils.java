/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.utilities;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;

public class DownloadUtils {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private RunUtils runUtils = new RunUtils();

    public void downloadPkgMgrFiles(String pkgMgrName, String pkgMgrVersion, String downloadDir, boolean throwExceptionOnFailedDownload, String downloadScriptName, Map<String, String> scriptArgs, String scriptsPath) throws DownloadFailedException {
        logger.debug(String.format("Looking for %s %s in %s", pkgMgrName, pkgMgrVersion, downloadDir));
        if (pkgMgrVersionFilesArePresent(downloadDir, pkgMgrName, pkgMgrVersion)) {
            // If package manager files we want are already present, don't download
            logger.debug(String.format("Found %s %s", pkgMgrName,  pkgMgrVersion));
            return;
        }

        runUtils.runScript(String.format("%s/%s", scriptsPath, downloadScriptName), scriptArgs);

        if (pkgMgrVersionFilesArePresent(downloadDir, pkgMgrName, pkgMgrVersion)) {
            logger.debug(String.format("Download of %s %s was successful.", pkgMgrName, pkgMgrVersion));
        } else {
            if (throwExceptionOnFailedDownload) {
                throw new DownloadFailedException(pkgMgrName);
            }
            logger.warn(String.format("Download of %s %s was not successful.", pkgMgrName, pkgMgrVersion));
        }
    }

    public boolean isUnEmptyDirectory(File dir) {
        return dir.exists() && dir.isDirectory() && null != dir.listFiles() && dir.listFiles().length != 0;
    }

    public boolean pkgMgrVersionFilesArePresent(String pkgMgrFilesPath, String name, String version) {
        File pkgMgrFiles = new File(pkgMgrFilesPath);
        pkgMgrFiles.mkdirs();
        File pkgMgrVersion = new File(pkgMgrFiles, version);
        pkgMgrVersion.mkdirs();
        if (isUnEmptyDirectory(pkgMgrVersion)) {
            File pkgMgrVersionFiles = new File(pkgMgrVersion, name);
            return isUnEmptyDirectory(pkgMgrVersionFiles);
        }
        return false;
    }
}
