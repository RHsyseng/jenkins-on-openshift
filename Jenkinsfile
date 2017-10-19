@Library('Utils') _

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                script {
                    /* NOTE: this does not work */
                    checkout([$class                           : 'GitSCM',
                              branches                         : scm.branches,
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class: 'PathRestriction', includedRegions: '^app/.*']],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : scm.userRemoteConfigs])
                }
            }
        }

        stage('Create Credentials') {
            steps {

                // Create a Jenkins Credential from OpenShift Secret
                // In this case the OpenShift service tokens for the
                // other environments.
                syncOpenShiftSecret params.STAGE_SECRET_NAME
            }
        }
        stage('Dev - MochaJS Test') {
            agent {
                label 'nodejs'
            }
            steps {
                dir('app') {
                    sh 'npm install'
                    sh 'npm test'
                }
            }
        }

        stage('Dev - OpenShift Template') {
            steps {
                script {
                    def openShiftApplyArgs = ""
                    env.VERSION = readFile('app/VERSION').trim()
                    env.TAG = "${env.VERSION}-${env.BUILD_NUMBER}"
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {
                            if (findFileChanges(params.APP_TEMPLATE_PATH) || !openshift.selector("template/${params.APP_DC_NAME}").exists()) {
                                // Apply the template object from JSON file
                                openshift.apply(readFile(params.APP_TEMPLATE_PATH))
                            } else {
                                openShiftApplyArgs = "--dry-run"
                            }
                            // Process the template and return the Map of the result
                            def model = openshift.process(params.IMAGE_STREAM_NAME,
                                    "-l app=${params.APP_DC_NAME}",
                                    "-p",
                                    "TAG=${env.TAG}",
                                    "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
                                    "REGISTRY_PROJECT=${params.REGISTRY_PROJECT}",
                                    "REGISTRY=${params.REGISTRY_URI}")

                            /* If the project's secret exists it must be removed at least in
                             * the case of using a database along with an application.
                             * Issue: Since the template is being process and applied with each
                             * job run the secret is also being updated since it contains generate
                             * values from the template.

                             * The section below confirms if the secret exists and if so removes the
                             * item from the collection.
                             */

                            if (openshift.selector("secret/${params.APP_DC_NAME}").exists()) {
                                def count = 0
                                for (item in model) {
                                    if (item.kind == 'Secret') {
                                        model.remove(count)
                                    }
                                    count++
                                }
                            }

                            createdObjects = openshift.apply(model, openShiftApplyArgs)
                        }
                    }
                }
            }
        }
        stage('Dev - Build Image') {
            steps {
                script {
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {
                            buildConfigs = createdObjects.narrow('bc')

                            def build = null
                            def buildErrorLog = "Build Error"

                            // there should only be one...
                            buildConfigs.withEach {
                                build = it.startBuild()
                            }

                            timeout(10) {
                                build.watch {
                                    // Wait until a build object is available
                                    return it.count() > 0
                                }
                                build.untilEach {
                                    // Wait until a build object is complete
                                    echo ">>>>> Enter Build Status Check <<<<<"
                                    def phase = it.object().status.phase


                                    echo ">>>>> Exit Build Status Check - Phase: ${phase} <<<<<"

                                    if (phase == "Complete") {
                                        return true
                                    } else if (phase == "Failed") {
                                        currentBuild.result = 'FAILURE'
                                        buildErrorLog = it.logs().actions[0].err
                                        return true
                                    } else {
                                        return false
                                    }
                                }
                            }
                            if (currentBuild.result == 'FAILURE') {
                                error(buildErrorLog)
                                return
                            }

                            /* Resolves issue with an ImageStream that won't initially
                             * import a tag
                             *
                             * Select the imagestream and check if that tags list contains a
                             * map with a key 'conditions'.  If conditions exist run `oc import-image`.
                             * This will perform the initial pull of the the ImageStreamTag.
                             *
                             * After the initial run confirm that conditions does not exist and exit the
                             * loop.
                             */

                            def importImage = false
                            def reCheck = false
                            timeout(10) {
                                createdObjects.narrow('is').untilEach {
                                    echo ">>>>> Enter ImageStream Check <<<<<"
                                    echo "${importImage}"
                                    echo "${reCheck}"

                                    if (importImage) {
                                        reCheck = true
                                    }
                                    for (item in it.object().status.tags) {
                                        if (item.containsKey('conditions')) {
                                            importImage = true
                                            reCheck = false
                                            openshift.raw("import-image ${it.name()}")
                                            sleep(30)
                                        }
                                    }
                                    echo ">>>>> Exit ImageStream Check <<<<<"
                                    echo "${importImage}"
                                    echo "${reCheck}"

                                    /* NXOR - if importImage and reCheck are both true or false
                                     * return true, else return false
                                     *
                                     * Multiple scenarios (importImage, reCheck):
                                     * - No import required (false, false)
                                     * - Imported image but needs to be rechecked (true, false)
                                     * - This should never happen (false, true)
                                     * - Imported image and checked (true, true)
                                     */

                                    if (!(importImage ^ reCheck))
                                        return true
                                    else return false
                                }
                            }

                            env.DEV_ROUTE = createdObjects.narrow('route').object().spec.host

                            echo "${env.DEV_ROUTE}"
                        }
                    }
                }
            }
        }
        stage('Dev - Rollout Latest') {
            steps {
                script {
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {

                            deploymentConfig = openshift.selector("dc", params.APP_DC_NAME)
                            deploymentConfig.rollout().latest()
                            timeout(10) {
                                deploymentConfig.rollout().status("-w")
                            }
                        }
                    }
                }
            }
        }
        stage('Stage - OpenShift Template') {
            environment {
                STAGE = credentials("${params.STAGE_SECRET_NAME}")
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
                            def openShiftApplyArgs = ""
                            if (findFileChanges(params.APP_TEMPLATE_PATH) || !openshift.selector("template/${params.APP_DC_NAME}").exists()) {
                                // Apply the template object from JSON file
                                openshift.apply(readFile(params.APP_TEMPLATE_PATH))
                            } else {
                                openShiftApplyArgs = "--dry-run"
                            }

                            // Process the template and return the Map of the result
                            def model = openshift.process(params.IMAGE_STREAM_NAME,
                                    "-l app=${params.APP_DC_NAME}",
                                    "-p",
                                    "TAG=${env.TAG}",
                                    "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
                                    "REGISTRY_PROJECT=${params.REGISTRY_PROJECT}",
                                    "REGISTRY=${params.REGISTRY_URI}")

                            /* If the project's secret exists it must be removed at least in
                             * the case of using a database along with an application.
                             * Issue: Since the template is being process and applied with each
                             * job run the secret is also being updated since it contains generate
                             * values from the template.

                             * The section below confirms if the secret exists and if so removes the
                             * item from the collection.
                             */

                            if (openshift.selector("secret/${params.APP_DC_NAME}").exists()) {
                                def count = 0
                                for (item in model) {
                                    if (item.kind == 'Secret') {
                                        model.remove(count)
                                    }
                                    count++
                                }
                            }
                            createdObjects = openshift.apply(model, openShiftApplyArgs)
                            // The stage environment does not need buildconfigs
                            createdObjects.narrow('bc').delete()
                        }
                    }
                }
            }
        }
        stage('Stage - Rollout') {
            environment {
                STAGE = credentials("${params.STAGE_SECRET_NAME}")
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
                            deploymentConfig = openshift.selector("dc", params.APP_DC_NAME)
                            deploymentConfig.rollout().latest()
                            timeout(10) {
                                deploymentConfig.rollout().status("-w")
                            }
                        }
                    }
                }
            }
        }
    }
}

// vim: ft=groovy
