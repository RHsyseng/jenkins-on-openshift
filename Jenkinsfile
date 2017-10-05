@Library('Utils') _

pipeline {
    agent any
/*
    parameters {
        string(name: 'IMAGE_STREAM_TAG', defaultValue: 'latest')
        string(name: 'REGISTRY_URI', defaultValue: 'docker-registry.engineering.redhat.com')
        string(name: 'DEV_PROJECT', defaultValue: 'lifecycle')
        string(name: 'STAGE_PROJECT', defaultValue: 'lifecycle' )
        string(name: 'STAGE_URI', defaultValue: 'insecure://openshift-ait.e2e.bos.redhat.com:8443')
        string(name: 'DEV_URI', defaultValue: 'insecure://osemaster.sbu.lab.eng.bos.redhat.com:8443')
        string(name: 'STAGE_SECRET', defaultValue: 'stage-api' )
    }
*/
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: scm.branches,
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'PathRestriction', includedRegions: '^app/.*']],
                    submoduleCfg: [],
                    userRemoteConfigs: scm.userRemoteConfigs])
            }
        }
        stage('Create Credentials') {
            steps {

                // Create a Jenkins Credential from OpenShift Secret
                // In this case the OpenShift service tokens for the
                // other environments.
                syncOpenShiftSecret params.STAGE_SECRET
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
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {

                            // Apply the template object from JSON file
                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))


                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${params.IMAGE_STREAM_TAG}",
                                            "REGISTRY=${params.REGISTRY_URI}",
                                            "PROJECT=${params.DEV_PROJECT}"))


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
                    openshift.withCluster(params.DEV_URI) {
                        openshift.withProject(params.DEV_PROJECT) {
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
                STAGE = credentials('${params.STAGE_SECRET}')
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
                            // Apply the template object from JSON file
                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))

                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${params.IMAGE_STREAM_TAG}",
                                            "REGISTRY=${params.REGISTRY_URI}",
                                            "PROJECT=${params.STAGE_PROJECT}"))

                            // The stage environment does not need buildconfigs
                            createdObjects.narrow('bc').delete()
                        }
                    }
                }
            }
        }
        stage('Stage - Rollout') {
            environment {
                STAGE = credentials('${params.STAGE_SECRET}')
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
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
