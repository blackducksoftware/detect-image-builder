package com.synopsys.integration.detect.imagebuilder

class PackageManager {
    private String name
    private String version
    private String detectPathProperty
    private String downloadScriptPath

    PackageManager(String name, String version, String detectPathProperty, String downloadScriptPath) {
        this.name = name
        this.version = version
        this.detectPathProperty = detectPathProperty
        this.downloadScriptPath = downloadScriptPath
    }

    String getName() {
        return name
    }

    String getVersion() {
        return version
    }

    String getDetectPathProperty() {
        return detectPathProperty
    }

    def downloadFiles() {
        GroovyShell shell = new GroovyShell()
        shell.run(new File(downloadScriptPath), new String[0])
    }
}