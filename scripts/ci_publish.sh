#!/usr/bin/env bash
set -xeuo pipefail
SCRIPT_DIR=$(dirname $(readlink -f $0))

export REGISTRY_URL=$1
export REGISTRY_USERNAME=$2
export REGISTRY_PASSWORD=$3
export DOCKER_VERSION_TAG=latest

cd "$SCRIPT_DIR/.." && ./gradlew :enclave:pushEnclaveImageDebugAsLatest -i
