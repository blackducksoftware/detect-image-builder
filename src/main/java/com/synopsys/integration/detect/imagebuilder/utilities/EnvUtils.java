/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.utilities;

import java.io.File;
import java.net.URISyntaxException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvUtils {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public String getEnv(String key, String defaultVal) {
        if (null != getEnv(key)) {
            return getEnv(key);
        }
        return defaultVal;
    }

    public String getEnv(String key) {
        return System.getenv(key);
    }

    @Nullable
    public String getResourcePath(String resource) {
        try {
            return new File(getClass().getClassLoader().getResource(resource).toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            logger.warn(String.format("Could not find script %s", resource));
            return null;
        }
    }

}

