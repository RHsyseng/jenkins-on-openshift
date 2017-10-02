@Library('Utils') _

pipeline {
    agent any
    stages {
        stage('Dev - MochaJS Test') {
            agent {
                label 'nodejs'
            }
            steps {

                git url: 'https://github.com/openshift/nodejs-ex'

                // Store the short sha to use with the ImageStreamTag
                script {
                    env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                }

                sh 'npm install'
                sh 'npm test'
            }
        }

        stage('Dev - OpenShift Template') {
            steps {

                git url: 'https://github.com/jcpowermac/jenkins-on-openshift', branch: 'template-mods'

                script {
                    openshift.withCluster() {
                        openshift.withProject() {

                            // Apply the template object from JSON file
                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))

                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.GIT_COMMIT}",
                                            "REGISTRY=docker-registry.engineering.redhat.com",
                                            "PROJECT=lifecycle"))


                        }
                    }
                }
            }
        }
        stage('Dev - Build Image') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {


                            buildConfigs = createdObjects.narrow('bc')

                            echo "${buildConfigs}"
                            def build = null

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
                                    return it.object().status.phase == "Complete"
                                }
                            }


                            env.IMAGE_STREAM_NAME = createdObjects.narrow('is').object().metadata.name
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
                    openshift.withCluster() {
                        openshift.withProject() {
                            deploymentConfigs = createdObjects.narrow('dc')
                            deploymentConfigs.withEach {
                                def rolloutManager = it.rollout()
                                rolloutManager.latest()
                            }

                            timeout(10) {
                                deploymentConfigs.withEach {
                                    it.rollout().status("-w")
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Dev - Tag') {
            steps {
                syncOpenShiftSecret 'registry-api'
                script {
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "registry-api", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

                        openshift.withCluster('insecure://internal-registry.host.prod.eng.rdu2.redhat.com:8443', env.PASSWORD) {
                            openshift.withProject('lifecycle') {
                                env.VERSION = readFile('app/VERSION').trim()

                                openshift.tag("${openshift.project()}/${env.IMAGE_STREAM_NAME}:${env.GIT_COMMIT}", "${openshift.project()}/${env.IMAGE_STREAM_NAME}:${env.VERSION}")
                            }
                        }
                    }

                }
            }
        }
        stage('Stage - OpenShift Template') {
            steps {

                // This method syncOpenShiftSecret will extract an OpenShift secret
                // and add it to a Jenkins Credential.

                syncOpenShiftSecret 'stage-api'
                git url: 'https://github.com/jcpowermac/jenkins-on-openshift', branch: 'template-mods'
                script {
                    // Use that newly created Jenkins credential to connect to an external
                    // cluster that is used for stage.

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "stage-api", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        openshift.withCluster('insecure://openshift-ait.e2e.bos.redhat.com:8443', env.PASSWORD) {
                            openshift.withProject('lifecycle') {
                                // Apply the template object from JSON file
                                openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))

                                createdObjects = openshift.apply(
                                        openshift.process("nodejs-mongo-persistent",
                                                "-p",
                                                "TAG=${env.GIT_COMMIT}",
                                                "REGISTRY=docker-registry.engineering.redhat.com",
                                                "PROJECT=lifecycle"))
                            }
                        }
                    }
                }
            }
        }
        stage('Stage - Rollout') {
            steps {
                script {
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "stage-api", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        openshift.withCluster('insecure://openshift-ait.e2e.bos.redhat.com:8443', env.PASSWORD) {
                            openshift.withProject('lifecycle') {
                                deploymentConfigs = createdObjects.narrow('dc')
                                deploymentConfigs.withEach {
                                    def rolloutManager = it.rollout()
                                    rolloutManager.latest()
                                }

                                timeout(10) {
                                    deploymentConfigs.withEach {
                                        it.rollout().status("-w")
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        stage('Production - Push Image') {
            steps {
                echo "Production - Push Image"
            }
        }
    }
}

// vim: ft=groovy

