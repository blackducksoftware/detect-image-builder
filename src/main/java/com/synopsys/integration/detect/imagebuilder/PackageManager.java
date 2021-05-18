/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder;

import java.util.List;


public class PackageManager {
    private String name;
    private List<String> versions;
    private String dockerfileName;
    private List<String> detectVersions;
    private List<String> javaVersions;

    public PackageManager(String name, List<String> versions,String dockerfileName, List<String> detectVersions, List<String> javaVersions) {
        this.name = name;
        this.versions = versions;
        this.dockerfileName = dockerfileName;
        this.detectVersions = detectVersions;
        this.javaVersions = javaVersions;
    }

    public String getName() {
        return name;
    }

    public List<String> getVersions() {
        return versions;
    }

    public String getDockerfileName() {
        return dockerfileName;
    }


    public List<String> getDetectVersions() {
        return detectVersions;
    }

    public List<String> getJavaVersions() {
        return javaVersions;
    }
}