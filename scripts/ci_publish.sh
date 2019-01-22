#!/usr/bin/env bash
set -xeuo pipefail
SCRIPT_DIR=$(dirname $(readlink -f $0))

export REGISTRY_URL=$1
export REGISTRY_USERNAME=$2
export REGISTRY_PASSWORD=$3

cd "$SCRIPT_DIR/.." && ./gradlew :rng:rng-enclave:pushEnclaveImageDebug -i
