#!/usr/bin/env bash
set -xeuo pipefail

SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

# Kill any lingering Gradle workers
ps aux | grep java | grep Gradle | awk '{ print $2; }' | xargs kill || true

cd ${SCRIPT_DIR}/..
./gradlew clean build -i --refresh-dependencies
