package com.synopsys.integration.detect.imagebuilder;

public class DownloadFailedException extends Exception {
    public DownloadFailedException(String downloadTarget) {
        super(String.format("Failed to download %s", downloadTarget));
    }
}
