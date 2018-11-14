#!/usr/bin/env bash
set -xeuo pipefail

SCRIPT_DIR=$(dirname $(readlink -f $0))

cd ${SCRIPT_DIR}/..
./gradlew clean test -i --refresh-dependencies
