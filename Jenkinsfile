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

                            openshift.apply(readFile('app/openshift/nodejs-mongodb-persistent.json'))

                            def createdObjects = openshift.apply(
                                    openshift.process( "nodejs-mongo-persistent",
                                                       "-p",
                                                       "TAG=${env.GIT_COMMIT}" )
                            )

                            def builds = createdObjects.narrow('bc').related('builds')

                            timeout(10) {
                                builds.watch {
                                    return it.count() > 0
                                }
                                builds.untilEach {
                                    return it.object().status.phase == "Complete"
                                }
                            }


                            //def pods = createdObjects.narrow('dc').related('pods')

                            timeout(10) {
                                createdObjects.narrow('dc').withEach {
                                    it.related('pods').untilEach {
                                        return it.object().status.phase == "Running"
                                    }
                                }
                            }

                            env.DEV_ROUTE = createdObjects.narrow('route').object().spec.host
                            println("${env.DEV_ROUTE}")

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

                syncOpenShiftSecret 'stage-api'
                script {
                    // Use that newly created Jenkins credential to connect to an external
                    // cluster that is used for stage.

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "stage", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        openshift.withCluster('https://openshift-ait.e2e.bos.redhat.com:8443', env.PASSWORD) {
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
        /*
        stage('Production - Promote Image') {
            steps {
                script {
                    env.PROMOTE_PROD = input message: 'Promote to Production'
                }
            }
        }
        */
    }
}

// vim: ft=groovy
