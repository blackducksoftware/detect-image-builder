package com.synopsys.integration.detect.imagebuilder

// Copy of bd-releng/bds-releng-build-images/jenkins-pipeline.groovy

//@Library('jenkins-shared-libraries')
//import com.blackducksoftware.jenkins.pipeline.Libraries

// Build job: Docker Images - releng-build

//def libraries = new Libraries()
libraries.loadEnvironment()
def utilities = libraries.getUtilities()
def httpUtils = libraries.getHttpUtils()
def jenkinsUtils = libraries.getJenkinsUtils(this)

if (!env.BRANCH){env.BRANCH='master'}
utilities.validateArgument("RELENG_IMAGE_PATHNAME", env.RELENG_IMAGE_PATHNAME)

def targetNode = 'docker'
def localRepoName = 'bds-releng-build-images'
def projectName = 'releng'
def fullLocalProjectPath = "${localRepoName}/${projectName}"

def libraryPath = "${env.RELENG_IMAGE_PATHNAME}/lib"

def baseOutputImageName = 'releng-base'
def outputRepo = 'blackducksoftware'
def baseImageName = "${outputRepo}/${baseOutputImageName}"
def outputImageName = baseImageName + ':'
def latestImageName = baseImageName + ':latest'
def baseContainerName = "${localRepoName}_${projectName}_build_"

def commitHashFileName = 'COMMITHASH'
def commitHashFile = "${env.RELENG_IMAGE_PATHNAME}/${commitHashFileName}"

def hubDetectPath = "${env.RELENG_IMAGE_PATHNAME}/SynopsysDetect"
def hubDetectFileName = 'detect.sh'
def detectCopyToNames = ['hub-detect.sh']
def hubDetectFile = "${hubDetectPath}/${hubDetectFileName}"
def detectJarLinkFile = 'detect.jar'
def detectVersion = ''
def detectJarVersionFile = "detect.version"
def detectScriptVersionMessage = ''

def bdsInstallerFileName = 'blackduck-installer'

def downstreamJobSearchToken = 'Infrastructure/Docker Images - '
def foundDownstreamJobs = []

env.SLACK_ERROR_CHANNEL = 'releng-jenkins-error'
env.SLACK_UPDATE_CHANNEL = 'bds-pop-jenkins'
def exitMessage

boolean shouldBuild = false

node(targetNode) {
    try{
        stage('Preparation') {
            utilities.prepareWorkspace(true)

            checkout scm: [$class: 'GitSCM', branches:
                    [[name: env.BRANCH]], userRemoteConfigs:
                                   [[url: "ssh://git@sig-gitlab.internal.synopsys.com/bd-releng/${localRepoName}.git"]], extensions:
                                   [[$class: 'LocalBranch', localBranch: env.BRANCH], [$class: 'RelativeTargetDirectory', relativeTargetDir: "${env.WORKSPACE}/${localRepoName}"]]]

            commitHash = sh(returnStdout: true, script: "cd ${fullLocalProjectPath} && git rev-parse HEAD").trim()
            sh """
         echo COMMITHASH=${commitHash} > git-hashes.properties &&
         mkdir -p ${fullLocalProjectPath}/${env.RELENG_IMAGE_PATHNAME} &&
         echo ${commitHash} > ${fullLocalProjectPath}/${commitHashFile}
         """
            archiveArtifacts 'git-hashes.properties'

            utilities.rmvDkrImage(latestImageName)
            utilities.dkrPullImage(latestImageName,8,15)
            baseContainerName += env.BUILD_NUMBER

            lastVersion = sh(returnStdout: true, script: """
                       docker inspect --format '{{ index .Config.Labels "version"}}' ${latestImageName}
                       """).trim()

            properties = libraries.getProperties(this, "version=${lastVersion}")
            outputImageName += properties.bumpedVersion
            utilities.rmvDkrImage(outputImageName)

            int imageExists = sh(returnStatus: true, script: "docker pull ${outputImageName}")
            if (imageExists == 0 && !env.FORCE_REBUILD.toBoolean()) {
                libraries.getSlack().sendNotification(env, currentBuild, env.SLACK_ERROR_CHANNEL, '', "FAILURE\n${env.JOB_NAME}\n${outputImageName} exists.")
                throw new RuntimeException("${outputImageName} exists. Fix or run with FORCE_REBUILD set to true")
            }

            dir ("${fullLocalProjectPath}/${hubDetectPath}") {
                httpUtils.curlDownload(env.DETECT_DOWNLOAD_URL,"",hubDetectFileName)
                sh "chmod +x ${hubDetectFileName}"

                for (String detectCopyToName : detectCopyToNames) {
                    assert sh(returnStatus: true, script: "ln -s ${hubDetectFileName} ${detectCopyToName}") == 0 : "Failed to make link for :: ${detectCopyToName}"
                }
            }

            createdImageHash = sh(returnStdout: true, script: """
                            docker create --name ${baseContainerName} --entrypoint="" ${latestImageName} /bin/bash
                            """).trim()
            assert createdImageHash.length() == 64 : "Create failed for ${latestImageName}"

            if (env.FORCE_REBUILD.toBoolean()) {
                println "env.FORCE_REBUILD set to true.\nRE-BUILDING"
                shouldBuild = true
            }
        }

        stage('Check for detect shell change') {
            try{
                sh "touch ${hubDetectFileName} && docker cp ${baseContainerName}:/${hubDetectFile} ${hubDetectFileName}"
            } catch(Exception e) {
                println "Hit an error getting the Detect script from the image. Ignoring"
            }

            imageDetectScript = readFile(hubDetectFileName)
            localDetectScript = readFile("${fullLocalProjectPath}/${hubDetectFile}")
            msg = sh(returnStdout: true, script: "ls -ltr ${hubDetectFileName} ${fullLocalProjectPath}/${hubDetectFile}").trim() + "\n"
            if (localDetectScript.equals(imageDetectScript)) {
                msg += "${hubDetectFileName} is the same, not re-building."
            } else {
                msg += "Differences found with ${hubDetectFileName}.\nRE-BUILDING"
                shouldBuild = true
            }
            println msg
        }

        stage('Check for detect jar change') {
            dir ("${fullLocalProjectPath}/${hubDetectPath}") {
                try{
                    sh """
             docker cp ${baseContainerName}:/${hubDetectPath}/${detectJarLinkFile} . &&
             docker cp ${baseContainerName}:/${hubDetectPath}/\$(readlink ${detectJarLinkFile}) .
             """
                } catch(Exception e) {
                    println "Hit an error getting the Detect jar from the image. Ignoring"
                }

                detectStdOut = sh(returnStdout: true, script: """
                          export DETECT_JAR_DOWNLOAD_DIR="\$(pwd)" &&
                          export DETECT_DOWNLOAD_ONLY=1 &&
                          ./${hubDetectFileName}
                          """).trim()
                detectScriptVersionMessage = detectStdOut.substring(0, detectStdOut.indexOf("\n"))
                msg = sh(returnStdout: true, script: "ls -ltr *jar || true").trim() + "\n"
                println detectStdOut
                if (detectStdOut.contains("You have already downloaded the latest file")) {
                    msg += "Latest jar already within container, not re-building"
                } else if (detectStdOut.contains("You don't have the current file")) {
                    sh "rm -f \$(readlink ${detectJarLinkFile})"
                    msg += "Container does not contain latest Detect jar.\nRE-BUILDING"
                    shouldBuild = true
                } else {
                    throw new RuntimeException("ERROR: Did not find expected messages from running detect.")
                }
                sh "rm -f ${detectJarLinkFile}"
            }
            println msg
        }

        stage('Check for BDS installer') {
            try {
                utilities.validateArgument("BDS_INSTALLER_VERSION", env.BDS_INSTALLER_VERSION)

                dir ("${fullLocalProjectPath}/${libraryPath}") {
                    bdsDownloadFile = "${bdsInstallerFileName}-${env.BDS_INSTALLER_VERSION}.jar"
                    httpUtils.curlDownload("https://sig-repo.synopsys.com/bds-integrations-release/com/synopsys/integration/${bdsInstallerFileName}/${env.BDS_INSTALLER_VERSION}/${bdsDownloadFile}")
                    sh "ln -s ${bdsDownloadFile} ${bdsInstallerFileName}.jar"
                }

                shouldBuild = true
            } catch(IllegalArgumentException ex) {
                assert ex.message.contains("Argument <BDS_INSTALLER_VERSION> not set") : "Expected arg not set error. Check configuration and input parameters. ${ex.message}"
                println "<BDS_INSTALLER_VERSION> was not provided, not including"
            } catch(Exception ex) {
                throw ex
            }
        }

        if (!shouldBuild) {
            exitMessage = 'ATTENTION:: Image does not need to be created. Exiting build.'
            currentBuild.result = "SUCCESS"
            return
        }

        stage('Build/Push image') {
            println "RE-BUILDING"

            dateStamp = sh(returnStdout: true, script: "date +%Y-%m-%d--%H_%M_%S").trim()

            dir ("${fullLocalProjectPath}/${hubDetectPath}") {
                detectJarFile = findFiles(glob: "synopsys-detect-*.jar")

                if (detectJarFile.size() != 1) {
                    throw new RuntimeException("ERROR: Could not find local Detect jar file.")
                } else {
                    sh "ln -s ${detectJarFile[0]} ${detectJarLinkFile}"
                    detectVersion = detectJarFile[0].toString().replace('synopsys-detect-','').replace('.jar','')
                    sh "echo ${detectVersion} > ${detectJarVersionFile}"
                }
            }

            utilities.rmvDkrContainerByFilter(baseContainerName)
            utilities.rmvDkrImage(latestImageName)

            sh "rm -rf ${fullLocalProjectPath}/${env.RELENG_IMAGE_PATHNAME}/*@tmp"

            sh """
         cd ${fullLocalProjectPath} &&
         find ${env.RELENG_IMAGE_PATHNAME}/* -exec ls -ld {} \\; &&
         docker build \
           --no-cache \
           --force-rm \
           --label build-date=${dateStamp} \
           --label build-tag=\"${env.BUILD_TAG}\" \
           --label commit-hash=${commitHash} \
           --label detect-script-version=\"${detectScriptVersionMessage}\" \
           --label detect-jar-version=\"${detectVersion}\" \
           --label image-name=${outputImageName} \
           --label vendor=\"Synopsys, Inc\" \
           --label version=${properties.bumpedVersion} \
           --build-arg "BDS_FILES_DIRNAME=${env.RELENG_IMAGE_PATHNAME}" \
            -f Dockerfile \
            -t ${outputImageName} . &&
         docker tag ${outputImageName} ${latestImageName}
         """

            utilities.pushImage(baseOutputImageName, properties.bumpedVersion, env.DOCKER_REGISTRY_SIG, env.ARTIFACTORY_DEPLOYER_USER, env.ARTIFACTORY_DEPLOYER_PASSWORD)
            utilities.pushImage(baseOutputImageName, "latest", env.DOCKER_REGISTRY_SIG, env.ARTIFACTORY_DEPLOYER_USER, env.ARTIFACTORY_DEPLOYER_PASSWORD)
        }

        stage('Trigger down stream') {
            jenkinsUtils.findJenkinsJobs(downstreamJobSearchToken).each{ job ->
                if (job.fullName == env.JOB_NAME || job.fullName =~ /python-build/) {
                    return
                }
                foundDownstreamJobs += job.fullName
            }

            for (String foundDownstreamJob : foundDownstreamJobs) {
                try{
                    if( !Boolean.valueOf(env.DRYRUN) ) {
                        println "Triggering build for : " + foundDownstreamJob + " at " + new Date()
                        build job: foundDownstreamJob, quietPeriod: 0, wait: true, parameters:[[$class: 'BooleanParameterValue', name: 'BUILD_ALL_IMAGES', value: true]]
                        println "Build complete for : " + foundDownstreamJob + " at " + new Date()
                    } else {
                        println "env.DRYRUN is true, NOT triggering build: " + foundDownstreamJob
                    }
                } catch(Exception e) {
                    libraries.getSlack().sendNotification(env, currentBuild, env.SLACK_ERROR_CHANNEL, '', "FAILURE\n${env.JOB_NAME} failed to run.\nTriggered by ${env.BUILD_URL}")
                    throw e
                }
            }
            if( !Boolean.valueOf(env.DRYRUN) ) {
                libraries.getSlack().sendNotification(env, currentBuild, env.SLACK_UPDATE_CHANNEL, '', "ATTENTION\nCreated a new Docker image for ${baseOutputImageName}::\n${outputImageName}")
            }

        }

    } catch(Exception e) {
        utilities.executeFinallyCmd("emailOnFailure", "UNSTABLE", {libraries.getEmail().emailOnFailure(env)})
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        utilities.executeFinallyCmd("rmvDkrContainerByFilter", "UNSTABLE", {utilities.rmvDkrContainerByFilter(baseContainerName)})
        utilities.executeFinallyCmd("rmvDkrImage", "FAILURE", {utilities.rmvDkrImage([latestImageName,outputImageName])})

        if (exitMessage) {
            println exitMessage
        }
    }
}

