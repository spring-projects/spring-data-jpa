#!/bin/bash -x

set -euo pipefail

mkdir -p /tmp/jenkins-home/.m2/spring-data-jpa
mkdir -p /tmp/jenkins-home/.m2/.gradle-enterprise
chown -R 1001:1001 .

export GRADLE_ENTERPRISE_CACHE_USERNAME=${GRADLE_ENTERPRISE_CACHE_USR}
export GRADLE_ENTERPRISE_CACHE_PASSWORD=${GRADLE_ENTERPRISE_CACHE_PSW}
export GRADLE_ENTERPRISE_ACCESS_KEY=${GRADLE_ENTERPRISE_ACCESS_KEY}

MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml  \
  -P${PROFILE} clean dependency:list test -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa

MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml clean -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa

chown -R 1001:1001  /tmp/jenkins-home/.m2/.gradle-enterprise
