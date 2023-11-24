def p = [:]
node {
	checkout scm
	p = readProperties interpolate: true, file: 'ci/pipeline.properties'
}

pipeline {
	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/3.2.x", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("test: baseline (main)") {
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
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
				DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
				DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
				TESTCONTAINERS_IMAGE_SUBSTITUTOR = 'org.springframework.data.jpa.support.ProxyImageNameSubstitutor'
			}
			steps {
				script {
					docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.docker']) {
						sh "PROFILE=all-dbs " +
							"JENKINS_USER_NAME=${p['jenkins.user.name']} " +
							"ci/test.sh"
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
				stage("test: baseline (hibernate 6.4.x snapshots)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES')}
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
						DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
						DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
						TESTCONTAINERS_IMAGE_SUBSTITUTOR = 'org.springframework.data.jpa.support.ProxyImageNameSubstitutor'
					}
					steps {
						script {
							docker.image(p['docker.java.next.image']).inside(p['docker.java.inside.docker']) {
								sh "PROFILE=all-dbs,hibernate-64-next " +
									"JENKINS_USER_NAME=${p['jenkins.user.name']} " +
									"ci/test.sh"
							}
						}
					}
				}
				stage("test: java.next (next)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES')}
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
						DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
						DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
						TESTCONTAINERS_IMAGE_SUBSTITUTOR = 'org.springframework.data.jpa.support.ProxyImageNameSubstitutor'
					}
					steps {
						script {
							docker.image(p['docker.java.next.image']).inside(p['docker.java.inside.docker']) {
								sh "PROFILE=all-dbs " +
									"JENKINS_USER_NAME=${p['jenkins.user.name']} " +
									"ci/test.sh"
							}
						}
					}
				}
				stage("test: eclipselink-next") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES')}
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
						DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
						DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
						TESTCONTAINERS_IMAGE_SUBSTITUTOR = 'org.springframework.data.jpa.support.ProxyImageNameSubstitutor'
					}
					steps {
						script {
							docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.docker']) {
								sh "PROFILE=all-dbs,eclipselink-next " +
									"JENKINS_USER_NAME=${p['jenkins.user.name']} " +
									"ci/test.sh"
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
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
				DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
				DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
			}
			steps {
				script {
					docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.basic']) {
						sh 'MAVEN_OPTS="-Duser.name=' + "${p['jenkins.user.name']}" + ' -Duser.home=/tmp/jenkins-home" ' +
								"DEVELOCITY_CACHE_USERNAME=${DEVELOCITY_CACHE_USR} " +
								"DEVELOCITY_CACHE_PASSWORD=${DEVELOCITY_CACHE_PSW} " +
								"GRADLE_ENTERPRISE_ACCESS_KEY=${DEVELOCITY_ACCESS_KEY} " +
								"./mvnw -s settings.xml -Pci,artifactory " +
								"-Dartifactory.server=${p['artifactory.url']} " +
								"-Dartifactory.username=${ARTIFACTORY_USR} " +
								"-Dartifactory.password=${ARTIFACTORY_PSW} " +
								"-Dartifactory.staging-repository=${p['artifactory.repository.snapshot']} " +
								"-Dartifactory.build-name=spring-data-jpa " +
								"-Dartifactory.build-number=${BUILD_NUMBER} " +
								'-Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa-enterprise ' +
								'-Dmaven.test.skip=true clean deploy -U -B '
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
