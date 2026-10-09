pipeline {
    agent any

    stages {

        stage('Maven Compile') {
            steps {
                bat 'mvnw.cmd -B clean compile'
            }
        }

        stage('Maven Test') {
            steps {
                bat 'mvnw.cmd -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Maven Package') {
            steps {
                // Reuses classes from Compile and tests from Test; nothing is rebuilt.
                bat 'mvnw.cmd -B package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                // Dockerfile is runtime-only and copies target/*.jar produced above.
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
