#!/usr/bin/env bash
set -xeuo pipefail
SCRIPT_DIR=$(dirname $(readlink -f $0))
NAMESPACE=oblivium-rng-enclave
ENCLAVE_IMAGE_TAG=$(cd "$SCRIPT_DIR/.." && git describe --long --abbrev=10)
TARGET_USER=$1
TARGET_HOST=$2
DOCKER_REGISTRY_URL=$3

# Put live current enclavelet image on remote host and wait for deployment

ssh $TARGET_USER@$TARGET_HOST << EOF  
  kubectl set image deployment/enclavelet-host-deployment enclavelet-host=$DOCKER_REGISTRY_URL/oblivium/oblivium-rng-enclave-server:$ENCLAVE_IMAGE_TAG -n $NAMESPACE
  kubectl rollout status deployment/enclavelet-host-deployment -n $NAMESPACE
EOF

