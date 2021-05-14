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

import com.synopsys.integration.detect.imagebuilder.download.GradleDownloader;
import com.synopsys.integration.detect.imagebuilder.download.MavenDownloader;

// This class specifies the versions of Detect, Java, and of various package managers that this image builder supports

class Supported {
    private String commonDockerfile = "common-dockerfile";
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
            commonDockerfile,
            new GradleDownloader(),
            detectVersions,
            javaVersions
            ));
        supported.add(new PackageManager(
            "maven",
            mavenVersions,
            "mvn --version",
            commonDockerfile,
            new MavenDownloader(),
            detectVersions,
            javaVersions
        ));
        supported.add(new PackageManager(
            "npm",
            Collections.singletonList(""), // currently installing node via apk which only supports latest
            "node --version && npm --version",
            "npm-dockerfile",
            null,
            detectVersions,
            javaVersions
        ));
    }

    List<PackageManager> getSupported() {
        return supported;
    }
}