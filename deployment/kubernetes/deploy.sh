#!/usr/bin/env bash
set -xeuo pipefail

REGISTRY_URL=$1
REGISTRY_USERNAME=$2
REGISTRY_PASSWORD=$3
TAG=$4
NAMESPACE=oblivium-rng-enclave
SCRIPT_DIR=$(dirname $(readlink -f $0))

# Create namespace
kubectl create namespace $NAMESPACE

# Deploy docker credentials
kubectl create secret docker-registry artifactory-docker-cred --docker-server=$REGISTRY_URL --docker-username=$REGISTRY_USERNAME --docker-password=$REGISTRY_PASSWORD -n $NAMESPACE

# Create enclavelet-host config map
kubectl create configmap enclavelet-host-config --from-file=$SCRIPT_DIR/enclavelet-host-config.yml -n $NAMESPACE

# Deploy sgx plugins and enclavelet-host pod 
sed s/{TAG}/$TAG/g $SCRIPT_DIR/sgx-device-plugin-daemonset.yml | kubectl apply -n $NAMESPACE -f -
sed s/{TAG}/$TAG/g $SCRIPT_DIR/enclavelet-host-deployment.yml | kubectl apply -n $NAMESPACE -f -

# Wait for deployment completion
kubectl rollout status deployment/enclavelet-host-deployment -n $NAMESPACE

# Create host service
kubectl apply -f $SCRIPT_DIR/enclavelet-host-service.yml -n $NAMESPACE
