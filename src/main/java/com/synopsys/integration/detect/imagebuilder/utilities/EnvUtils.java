/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.utilities;

public class EnvUtils {
    public String getEnv(String key, String defaultVal) {
        if (null != getEnv(key)) {
            return getEnv(key);
        }
        return defaultVal;
    }

    public String getEnv(String key) {
        return System.getenv(key);
    }

}

