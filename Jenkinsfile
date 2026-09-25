pipeline {
    agent any

    stages {

        stage('Maven Compile') {
            steps {
                bat 'mvnw.cmd clean compile'
            }
        }

        stage('Maven Test') {
            steps {
                bat 'mvnw.cmd test'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t apigateway-service:latest .'
            }
        }
    }

    post {
        success {
            mail(
                to: 'chrismervin715@gmail.com',
                subject: "SUCCESS: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """
Build Successful

Project: ${JOB_NAME}
Build: #${BUILD_NUMBER}
Status: SUCCESS
Build URL: ${BUILD_URL}
"""
            )
        }

        failure {
            mail(
                to: 'chrismervin715@gmail.com',
                subject: "FAILED: ${JOB_NAME} #${BUILD_NUMBER}",
                body: """
Build Failed

Project: ${JOB_NAME}
Build: #${BUILD_NUMBER}
Status: FAILURE
Build URL: ${BUILD_URL}
"""
            )
        }
    }
}
