#!/bin/bash -x

set -euo pipefail

MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml clean -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-jpa
