@Library('Utils') _

pipeline {
    agent any
    triggers {
        pollSCM('H/5 * * * *')
    }
    stages {
        stage('Dev - MochaJS Test' ) {
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
        stage('Dev - OpenShift Configuration') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            echo "Running OpenShift BuildConfig"
                        }
                    }
                }
            }
        }
        stage('Dev - Test') {
            steps {
                echo "Running dev test..."
            }
        }
        stage('Stage - OpenShift DeploymentConfig') {
            steps {

                // This method syncOpenShiftSecret will extract an OpenShift secret
                // and add it to a Jenkins Credential.

                syncOpenShiftSecret 'stage'
                script {
                    // Use that newly created Jenkins credential to connect to an external
                    // cluster that is used for stage.

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "stage", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        openshift.withCluster('https://osemaster.sbu.lab.eng.bos.redhat.com:8443', env.PASSWORD) {
                            openshift.withProject('lifecycle') {
                                echo "What are we doing here? - ${openshift.project()}"
                            }
                        }
                    }
                }
            }
        }
        stage('Stage - Test') {
            steps {
                echo "Stage - Test"
            }
        }
        stage('Production - Push Image') {
            steps {
                echo "Production - Push Image"
            }
        }
        stage('Production - Promote Image') {
            steps {
                script {
                    env.PROMOTE_PROD = input message: 'Promote to Production'
                }
            }
        }
    }
}
