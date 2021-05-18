/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// This class specifies the versions of Detect, Java, and of various package managers that this image builder supports

class Supported {
    private List<PackageManager> supported;

    private List<String> detectVersions = Arrays.asList("latest"); // default Detect version list
    private List<String> javaVersions = Arrays.asList("11"); // default Java version list
    private List<String> gradleVersions = Arrays.asList("6.7.1");
    private List<String> mavenVersions = Arrays.asList("3.8.1");


    Supported() {
        supported = new ArrayList<>();
        supported.add(new PackageManager(
            "gradle",
            gradleVersions,
            "gradle --version",
            "gradle-dockerfile",
            detectVersions,
            javaVersions
            ));
        supported.add(new PackageManager(
            "maven",
            mavenVersions,
            "mvn --version",
            "maven-dockerfile",
            detectVersions,
            javaVersions
        ));
        supported.add(new PackageManager(
            "npm",
            Collections.singletonList(""), // currently installing node via apk which only supports latest
            null, // command is hard-coded
            "npm-dockerfile",
            detectVersions,
            javaVersions
        ));
    }

    List<PackageManager> getSupported() {
        return supported;
    }
}