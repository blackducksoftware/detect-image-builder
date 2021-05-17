/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.download;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;
import com.synopsys.integration.detect.imagebuilder.utilities.DownloadUtils;
import com.synopsys.integration.detect.imagebuilder.utilities.EnvUtils;
import com.synopsys.integration.detect.imagebuilder.utilities.RunUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Downloader {
    Logger logger = LoggerFactory.getLogger(getClass());
    EnvUtils envUtils = new EnvUtils();
    RunUtils runUtils = new RunUtils();
    DownloadUtils downloadUtils = new DownloadUtils();
    abstract public void downloadFiles(String version, String destination, boolean throwExceptionOnFailedDownload, String scriptsPath) throws DownloadFailedException;
}
