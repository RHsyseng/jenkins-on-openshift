
pipeline {
    agent any
    triggers {
        cron('@daily')
    }
    stages {
        stage('jenkins-on-openshift-tests') {
            environment {
                OCP = credentials("openshift-auth")
            }
            agent {
                label 'ansible'
            }
            steps {
                dir('tests') {
                    script {
                        def cluster = ""
                        openshift.withCluster() {
                            cluster = openshift.cluster()
                        }

                        try {
                            sh "oc login ${cluster} --username ${env.OCP_USR} --password \"${env.OCP_PSW}\" --insecure-skip-tls-verify"
                            def token = sh(script: 'oc whoami -t', returnStdout: true).trim()

                            sh "ansible-playbook -e token=${token} -i hosts.ini ../ansible/main.yml"

                            sh "oc project jenkins-on-openshift-dev"
                            sh "oc rollout status dc/jenkins"

                            sh "oc start-build bc/app-pipeline --wait"
                            sh "oc start-build bc/release-pipeline -w -e RELEASE_VERSION_TAG=1.0-1 -e UPGRADE_WITHOUT_ASKING=yes"
                        }
                        catch(err) {
                            echo "${err}"
                        }
                        finally {
                            sh "oc delete project jenkins-on-openshift-dev jenkins-on-openshift-prod jenkins-on-openshift-registry jenkins-on-openshift-stage"
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            mail to: "${params.NOTIFY_EMAIL_LIST}",
            from: "${params.NOTIFY_EMAIL_FROM}",
            replyTo: "${params.NOTIFY_EMAIL_REPLYTO}",
            subject: "SUCCESS: jenkins-on-openshift",
            body: "Visit ${env.BUILD_URL} for details."
        }
        failure {
            mail to: "${params.NOTIFY_EMAIL_LIST}",
            from: "${params.NOTIFY_EMAIL_FROM}",
            replyTo: "${params.NOTIFY_EMAIL_REPLYTO}",
            subject: "FAILURE: jenkins-on-openshift",
            body: "Visit ${env.BUILD_URL} for details."
        }
    }
}
