pipeline {
	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/2.2.x", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("Test") {
			when {
				anyOf {
					branch '2.2.x'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: hibernate-next") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Phibernate-next clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: hibernate-53") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Phibernate-53 clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: hibernate-53-next") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Phibernate-53-next clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: hibernate-54") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Phibernate-54 clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: hibernate-54-next") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Phibernate-54-next clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: eclipselink-next") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Peclipselink-next clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: eclipselink-27") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Peclipselink-27 clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
				stage("test: eclipselink-27-next") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'rm -rf ?'
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Peclipselink-27-next clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B'
					}
				}
			}
		}
		stage('Release to artifactory') {
			when {
				anyOf {
					branch '2.2.x'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				docker {
					image 'adoptopenjdk/openjdk8:latest'
					label 'data'
					args '-v $HOME:/tmp/jenkins-home'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				sh 'rm -rf ?'
				sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,artifactory ' +
						'-Dartifactory.server=https://repo.spring.io ' +
						"-Dartifactory.username=${ARTIFACTORY_USR} " +
						"-Dartifactory.password=${ARTIFACTORY_PSW} " +
						"-Dartifactory.staging-repository=libs-snapshot-local " +
						"-Dartifactory.build-name=spring-data-jpa " +
						"-Dartifactory.build-number=${BUILD_NUMBER} " +
						'-Dmaven.test.skip=true clean deploy -U -B'
			}
		}
		stage('Publish documentation') {
			when {
				branch '2.2.x'
			}
			agent {
				docker {
					image 'adoptopenjdk/openjdk8:latest'
					label 'data'
					args '-v $HOME:/tmp/jenkins-home'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,distribute ' +
						'-Dartifactory.server=https://repo.spring.io ' +
						"-Dartifactory.username=${ARTIFACTORY_USR} " +
						"-Dartifactory.password=${ARTIFACTORY_PSW} " +
						"-Dartifactory.distribution-repository=temp-private-local " +
						'-Dmaven.test.skip=true clean deploy -U -B'
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
