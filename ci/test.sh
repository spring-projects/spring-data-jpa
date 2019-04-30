#!/usr/bin/env sh

set -euo pipefail

./mvnw -P${PROFILE} clean dependency:list test -Dsort -Dbundlor.enabled=false -B
