#!/usr/bin/env bash
set -xeuo pipefail
SCRIPT_DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))

cd "$SCRIPT_DIR/.."

./gradlew :samples:rng:rng-enclave:buildEnclaveImageDebug -i
if [[ ${OBLIVIUM_VERSIONING} = "release" ]]
then
  ./gradlew :samples:rng:rng-enclave:buildEnclaveImageRelease -i
fi

./gradlew :samples:rng:rng-enclave:pushEnclaveImageDebug -i
if [[ ${OBLIVIUM_VERSIONING} = "release" ]]
then
  ./gradlew :samples:rng:rng-enclave:pushEnclaveImageRelease -i
fi
