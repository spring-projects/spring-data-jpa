#!/usr/bin/env sh

set -euo pipefail

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

rm -rf $HOME/.m2/repository/org/springframework/data 2> /dev/null || :

cd spring-data-jpa-github

./mvnw clean dependency:list test -P${PROFILE} -Dsort -Dbundlor.enabled=false -B
