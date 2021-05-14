/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

public class DownloadFailedException extends Exception {
    public DownloadFailedException(String downloadTarget) {
        super(String.format("Failed to download %s", downloadTarget));
    }
}
