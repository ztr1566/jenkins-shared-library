// def call() is the main function that Jenkins will execute
def call(Map config) {
    pipeline {
        agent any // This allows the pipeline to run on any available Jenkins agent

        environment {
            // Jenkins credentials IDs that we will configure later in Jenkins UI
            DOCKER_HUB_CREDENTIALS = credentials('dockerhub-credentials')
            GITHUB_CREDENTIALS = credentials('github-credentials')

            // Extract username and password from the Docker Hub credentials
            DOCKER_HUB_USER = "${DOCKER_HUB_CREDENTIALS_USR}"
            DOCKER_HUB_PASS = "${DOCKER_HUB_CREDENTIALS_PSW}"

            // Your Docker Hub username
            DOCKER_HUB_REPO = "zizoo1566" // 
            
            // Your GitOps repository URL
            GITOPS_REPO_URL = "https://github.com/ztr1566/my-java-app-config.git" //
            
            IMAGE_NAME = "${DOCKER_HUB_REPO}/java-app"
            IMAGE_TAG = "build-${BUILD_NUMBER}" // Create a unique tag for each build
        }

        stages {
            stage('Cloning GitOps Repository') {
                steps {
                    script {
                        // We clone the GitOps repo to a subdirectory to use it later
                        sh "git clone ${GITOPS_REPO_URL} my-java-app-config"
                    }
                }
            }

            stage('Build with Maven') {
                steps {
                    // This command builds the Java application and skips the tests as per instructions
                    sh 'mvn clean package -Dmaven.test.skip=true' // [cite: 72]
                }
            }

            stage('Build Docker Image') {
                steps {
                    // Build a new Docker image with the unique tag
                    sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                }
            }

            stage('Push Docker Image to Docker Hub') {
                steps {
                    script {
                        // Login to Docker Hub using the credentials and push the image
                        sh "echo ${DOCKER_HUB_PASS} | docker login -u ${DOCKER_HUB_USER} --password-stdin"
                        sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }

            stage('Update GitOps Repository') {
                steps {
                    script {
                        // Go into the cloned GitOps repo directory
                        dir('my-java-app-config') {
                            // Update the image tag in the deployment.yaml file
                            // This command uses 'sed' to find and replace the image line
                            sh "sed -i 's|image: .*|image: ${IMAGE_NAME}:${IMAGE_TAG}|' deployment.yaml"

                            // Configure git with credentials to be able to push
                            sh "git config user.email 'ziad@example.com'"
                            sh "git config user.name 'Ziad'"
                            
                            // Commit and push the change to the GitOps repo
                            sh "git add deployment.yaml"
                            sh "git commit -m 'Update image to ${IMAGE_TAG}'"
                            
                            // Use the GitHub credentials to push the changes
                            sh "git push https://${GITHUB_CREDENTIALS}@github.com/ztr1566/my-java-app-config.git HEAD:main" //
                        }
                    }
                }
            }
        }

        post {
            always {
                // Clean up the workspace after the pipeline finishes
                cleanWs()
                sh 'rm -rf my-java-app-config'
            }
        }
    }
}
