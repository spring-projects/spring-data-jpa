pipeline {
    agent none

    triggers {
        pollSCM 'H/10 * * * *'
        upstream(upstreamProjects: "spring-data-commons/1.13.x", threshold: hudson.model.Result.SUCCESS)
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
                stage("test: hibernate-41") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-41 ci/test.sh"
                    }
                }
                stage("test: hibernate-42") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-42 ci/test.sh"
                    }
                }
                stage("test: hibernate-42-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-42-next ci/test.sh"
                    }
                }
                stage("test: hibernate-43") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-43 ci/test.sh"
                    }
                }
                stage("test: hibernate-43-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-43-next ci/test.sh"
                    }
                }
                stage("test: hibernate-5") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-5 ci/test.sh"
                    }
                }
                stage("test: hibernate-51") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-51 ci/test.sh"
                    }
                }
                stage("test: hibernate-51-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-51-next ci/test.sh"
                    }
                }
                stage("test: hibernate-52") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-52 ci/test.sh"
                    }
                }
                stage("test: hibernate-52-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:alpine'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "PROFILE=hibernate-52-next ci/test.sh"
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
                branch '1.11.x'
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
