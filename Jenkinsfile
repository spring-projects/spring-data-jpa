pipeline {
    agent none

    triggers {
        pollSCM 'H/10 * * * *'
        upstream(upstreamProjects: "spring-data-commons/master", threshold: hudson.model.Result.SUCCESS)
    }

    options {
        disableConcurrentBuilds()
    }
    
    stages {
        stage("Test") {
            parallel {
                stage("test: baseline") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=none ci/test.sh"
                    }
                }
                stage("test: hibernate-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-next ci/test.sh"
                    }
                }
                stage("test: hibernate-53") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-53 ci/test.sh"
                    }
                }
                stage("test: hibernate-53-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-53-next ci/test.sh"
                    }
                }
                stage("test: hibernate-54") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-54 ci/test.sh"
                    }
                }
                stage("test: hibernate-54-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-54-next ci/test.sh"
                    }
                }
                stage("test: eclipselink-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=eclipselink-next ci/test.sh"
                    }
                }
                stage("test: eclipselink-27") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=eclipselink-27 ci/test.sh"
                    }
                }
                stage("test: eclipselink-27-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=eclipselink-27-next ci/test.sh"
                    }
                }
            }
        }
        stage('Release to artifactory') {
            when {
                branch 'issue/*'
            }
            agent {
                docker {
                    image 'adoptopenjdk/openjdk8:alpine'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh "USERNAME=${ARTIFACTORY_USR} PASSWORD=${ARTIFACTORY_PSW} PROFILE=ci,snapshot ci/build.sh"
            }
        }
        stage('Release to artifactory with docs') {
            when {
                branch 'master'
            }
            agent {
                docker {
                    image 'adoptopenjdk/openjdk8:alpine'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh "USERNAME=${ARTIFACTORY_USR} PASSWORD=${ARTIFACTORY_PSW} PROFILE=ci,snapshot ci/build.sh"
            }
        }
    }

    post {
        changed {
            script {
                slackSend(
                        color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
                        channel: '#spring-data-dev',
                        message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
                emailext(
                        subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
                        mimeType: 'text/html',
                        recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                        body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
            }
        }
    }
}
