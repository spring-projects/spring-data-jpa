#!/usr/bin/env sh

set -euo pipefail

./mvnw -P${PROFILE} -DskipTests=true clean deploy -B
