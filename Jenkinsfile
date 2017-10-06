@Library('Utils') _

pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                script {
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
                //git url: 'https://github.com/RHsyseng/jenkins-on-openshift.git'
                dir('app') {
                    sh 'npm install'
                    sh 'npm test'
                }
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
                            openshift.apply(readFile(params.APP_TEMPLATE_PATH))


                            createdObjects = openshift.apply(
                                    openshift.process(params.IMAGE_STREAM_NAME,
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
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

                            //env.IMAGE_STREAM_NAME = createdObjects.narrow('is').object().metadata.name
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
                STAGE = credentials('${params.STAGE_SECRET_NAME}')
            }
            steps {
                script {
                    openshift.withCluster(params.STAGE_URI, env.STAGE_PSW) {
                        openshift.withProject(params.STAGE_PROJECT) {
                            // Apply the template object from JSON file
                            openshift.apply(readFile(params.APP_TEMPLATE_PATH))

                            createdObjects = openshift.apply(
                                    openshift.process("nodejs-mongo-persistent",
                                            "-p",
                                            "TAG=${env.TAG}",
                                            "IMAGESTREAM_TAG=${params.IMAGE_STREAM_LATEST_TAG}",
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
                STAGE = credentials('${params.STAGE_SECRET_NAME}')
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
