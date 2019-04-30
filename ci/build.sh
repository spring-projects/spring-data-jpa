#!/usr/bin/env sh

set -euo pipefail

./mvnw -P${PROFILE} -Dmaven.test.skip=true clean deploy -B
