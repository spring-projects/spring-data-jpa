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
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: hibernate-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Phibernate-next clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: hibernate-53") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Phibernate-53 clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: hibernate-53-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Phibernate-53-next clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: hibernate-54") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Phibernate-54 clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: hibernate-54-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Phibernate-54-next clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: eclipselink-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Peclipselink-next clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: eclipselink-27") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Peclipselink-27 clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
                    }
                }
                stage("test: eclipselink-27-next") {
                    agent {
                        docker {
                            image 'adoptopenjdk/openjdk8:latest'
                            args '-v $HOME/.m2:/root/.m2'
                        }
                    }
                    steps {
                        sh "./mvnw -Peclipselink-27-next clean dependency:list test -Dsort -Dbundlor.enabled=false -B"
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
                    image 'adoptopenjdk/openjdk8:latest'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh "USERNAME=${ARTIFACTORY_USR} PASSWORD=${ARTIFACTORY_PSW} ./mvnw -Pci,snapshot -DskipTests=true clean deploy -B"
            }
        }
        stage('Release to artifactory with docs') {
            when {
                branch 'master'
            }
            agent {
                docker {
                    image 'adoptopenjdk/openjdk8:latest'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }

            environment {
                ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
            }

            steps {
                sh "USERNAME=${ARTIFACTORY_USR} PASSWORD=${ARTIFACTORY_PSW} ./mvnw -Pci,snapshot -DskipTests=true clean deploy -B"
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
