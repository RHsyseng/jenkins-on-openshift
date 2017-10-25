/* Import Pipeline Global Library.
 * Implemented in src/com/redhat/ and vars/
 */
@Library('Utils') _

pipeline {
    agent any
    stages {

        /** Create Credentials
         *
         * Create a Jenkins Credential from OpenShift Secret
         * In this case the OpenShift service tokens for the other
         * environments.
         */

        stage('Create Credentials') {
            steps {

                /* This method is implemented in vars/syncOpenShiftSecret.groovy */
                syncOpenShiftSecret params.STAGE_SECRET_NAME
            }
        }

        /** Dev - MochaJS Test
         *
         *  Using agent labeled `nodejs` which is defined in the Kubernetes Jenkins plugin will
         *  launch a nodejs pod to run the steps section actions.
         */

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

        /** Dev - OpenShift Template
         *
         * This stage applies the template that is available in the project repository.
         * Processes the template using parameters defined in the Jenkins Job
         * And finally applies the objects returned from processing
         */
        stage('Dev - OpenShift Template') {
            steps {
                script {
                    def openShiftApplyArgs = ""
                    updateBuildConfig = false

                    /* Read the version but make sure there is not any characters we do not expect */
                    env.VERSION = readFile('app/VERSION').trim()
                    env.TAG = "${env.VERSION}-${env.BUILD_NUMBER}"
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {

                            /* If either there is a change in the OpenShift template
                             * or there is no template object available in OpenShift
                             * create the template.
                             *
                             * Otherwise use the `oc` command option `--dry-run` which
                             * will still allow the return of the objects from the openshift.apply()
                             * method but not actually apply those objects.
                             *
                             * findFileChanges method is implemented in vars/findFileChanges.groovy
                             */


                            if (findFileChanges(params.APP_TEMPLATE_PATH)
                                    || !openshift.selector("template/${params.APP_DC_NAME}").exists()) {

                                /* Apply the template object from JSON file */
                                openshift.apply(readFile(params.APP_TEMPLATE_PATH))
                                echo ">>>>> OpenShift Template Changed <<<<<"
                            } else {
                                openShiftApplyArgs = "--dry-run"
                                updateBuildConfig = true
                                echo ">>>>> OpenShift Template Unchanged <<<<<"
                            }

                            /* Process the template and return the Map of the result */
                            def model = openshift.process(params.IMAGE_STREAM_NAME,
                                    "-l app=${params.APP_DC_NAME}",
                                    "-p",
                                    "TAG=${env.TAG}",
                                    "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
                                    "REGISTRY_PROJECT=${params.REGISTRY_PROJECT}",
                                    "REGISTRY=${params.REGISTRY_URI}")

                           	/* If the project's secret exists it must be removed at least in
                             * the case of using a database along with an application.
							 *
                             * The section below confirms if the secret exists and if so removes the
                             * item from the collection.
                             *
                             * Issue: When using a template with generated values each process of that
                             * template will change the generated values.  If a DeploymentConfig is not
                             * redeployed those changes will not be propagated.
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

                            echo ">>>>> Enter OpenShift Apply Template Model <<<<<"
                            createdObjects = openshift.apply(model, openShiftApplyArgs)
                            echo ">>>>> Exit OpenShift Apply Template Model <<<<<"
                        }
                    }
                }
            }
        }

        /** Dev - Build Image
         *
         * This stage executes the BuildConfig with `oc start-build` building the application container image.
         */
        stage('Dev - Build Image') {
            steps {
                script {
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {

                            /* Select only BuildConfig from the objects that were created from the template
                             * process and apply
                             */
                            def buildConfig = openshift.selector("buildconfig/${params.APP_DC_NAME}").object()
                            buildConfigs = createdObjects.narrow('bc')
                            imageName = "${((String) buildConfig.spec.output.to.name).split(':')[0]}:${env.TAG}"

                            if (updateBuildConfig) {
                                echo ">>>>> Enter OpenShift Apply BuildConfig <<<<<"
                                buildConfig.spec.output.to.name = imageName
                                openshift.apply(buildConfig)
                                openshift.raw("annotate", "buildconfig/${params.APP_DC_NAME}",
                                        "kubectl.kubernetes.io/last-applied-configuration-")
                                echo ">>>>> Exit OpenShift Apply BuildConfig <<<<<"
                            }

                            def build = null
                            def buildErrorLog = "Build Error"

                            /* Execute a `oc start-build` for each available BuildConfig */
                            buildConfigs.withEach {
                                build = it.startBuild()
                            }

                            timeout(10) {
                                build.watch {
                                    /* Wait until a build object is available */
                                    return it.count() > 0
                                }
                                build.untilEach {
                                    /* Wait until a build object is complete */

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

                            /* If the currentBuild result is failure print the OpenShift Build error log
                             * and exit the job.
                             */
                            if (currentBuild.result == 'FAILURE') {
                                error(buildErrorLog)
                                return
                            }

                            openshift.tag("--source=docker",
                                    "${imageName}",
                                    "${openshift.project()}/${params.IMAGE_STREAM_NAME}:${params.IMAGE_STREAM_LATEST_TAG}")

                            /* Resolves issue with an ImageStream that won't initially
                             * import a tag
                             *
                             * Select the ImageStream and check if that tags list contains a
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
                                    echo ">>>>> importImage: ${importImage} reCheck: ${reCheck} <<<<<"

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
                                    echo ">>>>> importImage: ${importImage} reCheck: ${reCheck} <<<<<"

                                    /* NXOR - if importImage and reCheck are both true or false
                                     * return true, else return false
                                     *
                                     * Multiple scenarios (importImage, reCheck):
                                     * - No import required (false, false)
                                     * - Imported image but needs to be rechecked (true, false)
                                     * - This should never happen (false, true)
                                     * - Imported image and checked (true, true)
                                     */

                                    return (!(importImage ^ reCheck))
                                }
                            }

                            /* If we need the URL to the application that we are deploying */
                            env.DEV_ROUTE = createdObjects.narrow('route').object().spec.host

                            echo "Application URL: ${env.DEV_ROUTE}"
                        }
                    }
                }
            }
        }

        /** Dev - Rollout Latest
         *
         * This stage rolls out the OpenShift DeploymentConfig defining the application.
         * It will wait until the rollout completes or fails.
         */
        stage('Dev - Rollout Latest') {
            steps {
                script {
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {

                            /* Select the OpenShift DeploymentConfig object
                             * Initiate a `oc rollout latest` command
                             * Watch the status until the rollout is complete using the `oc`
                             * option `-w` to watch
                             */
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
        /** Stage - OpenShift Template
         *
         * This is generally duplicated from the Dev - OpenShift Template
         * See inline documentation above.
         */
        stage('Stage - OpenShift Template') {
            environment {
                STAGE = credentials("${params.STAGE_SECRET_NAME}")
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
                            def openShiftApplyArgs = ""
                            if (findFileChanges(params.APP_TEMPLATE_PATH)
                                    || !openshift.selector("template/${params.APP_DC_NAME}").exists()) {
                                openshift.apply(readFile(params.APP_TEMPLATE_PATH))
                            } else {
                                openShiftApplyArgs = "--dry-run"
                                openshift.tag("--source=docker",
                                        "${imageName}",
                                        "${openshift.project()}/${params.IMAGE_STREAM_NAME}:${params.IMAGE_STREAM_LATEST_TAG}")
                            }

                            def model = openshift.process(params.IMAGE_STREAM_NAME,
                                    "-l app=${params.APP_DC_NAME}",
                                    "-p",
                                    "TAG=${env.TAG}",
                                    "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
                                    "REGISTRY_PROJECT=${params.REGISTRY_PROJECT}",
                                    "REGISTRY=${params.REGISTRY_URI}")

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

                            /* The stage environment does not need OpenShift BuildConfig objects */
                            if (createdObjects.narrow('bc').exists()) {
                                createdObjects.narrow('bc').delete()
                            }
                        }
                    }
                }
            }
        }
        /** Stage - Rollout
         *
         * This is generally duplicated from the Dev - Rollout
         * See inline documentation above.
         */
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
