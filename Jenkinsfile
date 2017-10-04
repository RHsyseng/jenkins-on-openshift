@Library('Utils') _

pipeline {
    agent any

    environment {
        IMAGE_STREAM_TAG = "latest"
        REGISTRY = "docker-registry.engineering.redhat.com"
        PROJECT = "lifecycle"
        STAGE_URI = "insecure://openshift-ait.e2e.bos.redhat.com:8443"
    }
    stages {
        stage('Create Credentials') {
            steps {

                // Create a Jenkins Credential from OpenShift Secret
                // In this case the OpenShift service tokens for the
                // other environments.
                syncOpenShiftSecret 'registry-api'
                syncOpenShiftSecret 'stage-api'
            }
        }
        stage('Dev - MochaJS Test') {
            agent {
                label 'nodejs'
            }
            steps {
                git url: 'https://github.com/openshift/nodejs-ex'
                //dir('app')

                sh 'npm install'
                sh 'npm test'
            }
        }
        stage('Dev - OpenShift Template') {
            steps {

                script {
                    env.VERSION = readFile('app/VERSION').trim()
                    env.TAG = "${env.VERSION}-${env.BUILD_NUMBER}"
                    openshift.withCluster() {
                        openshift.withProject() {

                            // Apply the template object from JSON file
                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))


                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${env.IMAGE_STREAM_TAG}",
                                            "REGISTRY=${env.REGISTRY}",
                                            "PROJECT=${env.PROJECT}"))


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
                                if (!it.name().startsWith("mongo")) {
                                    it.rollout().latest()
                                }
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
        stage('Stage - OpenShift Template') {
            environment {
                STAGE = credentials('stage-api')
            }
            steps {
                script {
                    openshift.withCluster(env.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(env.PROJECT) {
                            // Apply the template object from JSON file
                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))

                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${IMAGE_STREAM_TAG}",
                                            "REGISTRY=${REGISTRY}",
                                            "PROJECT=${PROJECT}"))

                            // The stage environment does not need buildconfigs
                            createdObjects.narrow('bc').delete()
                        }
                    }
                }
            }
        }
        stage('Stage - Rollout') {
            environment {
                STAGE = credentials('stage-api')
            }
            steps {
                script {
                    openshift.withCluster(env.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(env.PROJECT) {
                            deploymentConfigs = createdObjects.narrow('dc')
                            deploymentConfigs.withEach {
                                if (!it.name().startsWith("mongo")) {
                                    it.rollout().latest()
                                }
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
}

// vim: ft=groovy

