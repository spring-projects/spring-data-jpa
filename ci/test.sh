#!/bin/bash -x

set -euo pipefail

mkdir -p /tmp/jenkins-home/.m2/spring-data-jpa
mkdir -p /tmp/jenkins-home/.m2/.develocity

export JENKINS_USER=${JENKINS_USER_NAME}

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml  \
  -P${PROFILE} clean dependency:list test -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa -Ddevelocity.storage.directory=/tmp/jenkins-home/.develocity-root

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml clean -Dscan=false -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa -Ddevelocity.storage.directory=/tmp/jenkins-home/.develocity-root
