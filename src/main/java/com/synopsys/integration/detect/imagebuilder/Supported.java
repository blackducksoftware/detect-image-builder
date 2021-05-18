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
    private List<PackageManager> supportedPkgMgrs;

    private List<String> detectVersions = Arrays.asList("latest"); // default Detect version list
    private List<String> javaVersions = Arrays.asList("11"); // default Java version list
    private List<String> gradleVersions = Arrays.asList("6.7.1");
    private List<String> mavenVersions = Arrays.asList("3.8.1");


    Supported() {
        supportedPkgMgrs = new ArrayList<>();
        supportedPkgMgrs.add(new PackageManager(
            "gradle",
            gradleVersions,
            "gradle-dockerfile",
            detectVersions,
            javaVersions
            ));
        supportedPkgMgrs.add(new PackageManager(
            "maven",
            mavenVersions,
            "maven-dockerfile",
            detectVersions,
            javaVersions
        ));
        supportedPkgMgrs.add(new PackageManager(
            "npm",
            Collections.singletonList(""), // currently installing node via apk which only supports latest
            "npm-dockerfile",
            detectVersions,
            javaVersions
        ));
    }

    public List<String> getSupportedDetectVersions() {
        return detectVersions;
    }

    public List<String> getSupportedJavaVersions() {
        return javaVersions;
    }

    List<PackageManager> getSupportedPackageManagers() {
        return supportedPkgMgrs;
    }
}