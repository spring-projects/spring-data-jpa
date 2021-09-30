def props = null // Load this in the first stage

pipeline {

	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/main", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {

	    stage("Initialize") {
	        agent {
	            label 'data'
	        }
	        options { timeout(time: 30, unit: 'MINUTES') }
	        steps {
	            script {
	                props = readProperties interpolate: true, file: 'ci/configuration.properties'
	            }
	        }
	    }

		stage("test: baseline (java.baseline)") {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 30, unit: 'MINUTES') }
			environment {
				DOCKER_HUB = credentials("${props['docker.credentials.ref']}")
				ARTIFACTORY = credentials("${props['artifactory.credentials.ref']}")
			}
			steps {
				script {
					docker.withRegistry('', props['docker.credentials.ref']) {
						docker.image(props['docker.java.baseline.image']).inside(props['docker.java.inside.root']) {
                            sh "docker login --username ${DOCKER_HUB_USR} --password ${DOCKER_HUB_PSW}"
                            sh "mkdir -p ${props['maven.repo.base']}/spring-data-jpa"
                            sh "chown -R 1001:1001 ."
                            sh "${props['docker.java.env']} ./mvnw -s settings.xml " +
                                  "-Pall-dbs clean dependency:list test -Dsort -U -B -Dmaven.repo.local=${props['maven.repo.base']}/spring-data-jpa"
                            sh "${props['docker.java.env']} ./mvnw -s settings.xml clean -Dmaven.repo.local=${props['maven.repo.base']}/spring-data-jpa"
						}
					}
				}
			}
		}

		stage("Test other configurations") {
			when {
				beforeAgent(true)
				allOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline (java.next)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
			        	ARTIFACTORY = credentials("${props['artifactory.credentials.ref']}")
					}
					steps {
						script {
							docker.withRegistry('', props['docker.credentials.ref']) {
								docker.image(props['docker.java.next.image']).inside(props['docker.java.inside']) {
                                    sh "${props['docker.java.env']} ./mvnw -s settings.xml -Pjava11 clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B"
								}
							}
						}
					}
				}

				stage("test: baseline (java.lts)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
			        	ARTIFACTORY = credentials("${props['artifactory.credentials.ref']}")
					}
					steps {
						script {
							docker.withRegistry('', props['docker.credentials.ref']) {
								docker.image(props['docker.java.lts.image']).inside(props['docker.java.inside']) {
                                    sh "${props['docker.java.env']} ./mvnw -s settings.xml -Pjava11 clean dependency:list test -Dsort -Dbundlor.enabled=false -U -B"
								}
							}
						}
					}
				}
			}
		}

		stage('Release to artifactory') {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
                ARTIFACTORY = credentials("${props['artifactory.credentials.ref']}")
			}

			steps {
				script {
					docker.withRegistry('', props['docker.credentials.ref']) {
						docker.image(props['docker.java.baseline.image']).inside(props['docker.java.inside']) {
                            sh "${props['docker.java.env']} ./mvnw -s settings.xml -Pci,artifactory " +
                                    '-Dartifactory.server=https://repo.spring.io ' +
                                    "-Dartifactory.username=${ARTIFACTORY_USR} " +
                                    "-Dartifactory.password=${ARTIFACTORY_PSW} " +
                                    "-Dartifactory.staging-repository=libs-snapshot-local " +
                                    "-Dartifactory.build-name=spring-data-jpa " +
                                    "-Dartifactory.build-number=${BUILD_NUMBER} " +
                                    '-Dmaven.test.skip=true clean deploy -U -B'
						}
					}
				}
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
