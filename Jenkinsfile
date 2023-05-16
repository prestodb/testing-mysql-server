pipeline {

    agent {
        kubernetes {
            yamlFile 'jenkins/agent.yaml'
            defaultContainer 'maven'
        }
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '100'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage("Setup") {
            steps {
                sh '''
                    apt update
                    apt install -y binutils cpio libaio1 libaio-dev libnuma-dev rpm2cpio tree xz-utils
                    printenv | sort
                '''
            }
        }

        stage ("Maven") {
            steps {
                sh '''
                    mvn verify
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "**/surefire-reports/",
                             allowEmptyArchive: true
            junit '**/junitreports/*.xml'
        }
        fixed {
            slackSend(color: "good", message: "fixed ${RUN_DISPLAY_URL}")
        }
        failure {
            slackSend(color: "danger", message: "failure ${RUN_DISPLAY_URL}")
        }
    }
}
