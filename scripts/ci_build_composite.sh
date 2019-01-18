#!/usr/bin/env bash
set -xeuo pipefail

# This script expects the oblivium project to be checked out in the `oblivium` subdirectory.

SCRIPT_DIR=$(dirname $(readlink -f $0))
export REGISTRY_USERNAME=$1
export REGISTRY_PASSWORD=$2
export REGISTRY_URL=$3
export MAVEN_URL=$4
export MAVEN_REPOSITORY=$5
export MAVEN_USERNAME=$6
export MAVEN_PASSWORD=$7
export OBLIVIUM_VERSION=$(cd "$SCRIPT_DIR/../oblivium" && git describe --long --abbrev=10)}

source $SCRIPT_DIR/../oblivium/scripts/ci_exclude_native.sh

# Login and pull the current build image
docker login $REGISTRY_URL -u $REGISTRY_USERNAME -p $REGISTRY_PASSWORD
docker pull $REGISTRY_URL/oblivium/oblivium-build

mkdir -p /home/$(id -un)/.gradle
mkdir -p /home/$(id -un)/.ccache

CODE_HOST_DIR=$PWD
export CONTAINER_NAME=$(echo "code${CODE_HOST_DIR}" | sed -e 's/[^a-zA-Z0-9_.-]/_/g')
CODE_DOCKER_DIR="/${CONTAINER_NAME}"

docker run --rm \
       -u $(id -u):$(id -g) \
       --network host \
       --group-add $(cut -d: -f3 < <(getent group docker)) \
       -v /home/$(id -un)/.gradle:/gradle \
       -v /home/$(id -un)/.ccache:/home/.ccache \
       -v $SCRIPT_DIR/..:$CODE_DOCKER_DIR \
       -v /var/run/docker.sock:/var/run/docker.sock \
       -e GRADLE_USER_HOME=/gradle \
       -e MAVEN_URL=$MAVEN_URL \
       -e MAVEN_REPOSITORY=$MAVEN_REPOSITORY \
       -e MAVEN_USERNAME=$MAVEN_USERNAME \
       -e MAVEN_PASSWORD=$MAVEN_PASSWORD \
       -e OBLIVIUM_VERSION=$OBLIVIUM_VERSION \
       $REGISTRY_URL/oblivium/oblivium-build \
       bash -c \
       "cd $CODE_DOCKER_DIR && ./gradlew $EXCLUDE_NATIVE build -i"
