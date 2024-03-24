#!/bin/bash

DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
USER_DIR=$(pwd)

function unpack() {
  FOLDER=$1
  
  CURRENT=$(pwd)

  cd $FOLDER/build/libs
  cp -v *.jar app.jar
  java -jar -Djarmode=layertools app.jar extract
  rm app.jar
  cd $CURRENT
}
function build() {
  FOLDER=$1
  NAME=$2
  VERSION=${3:-latest}

  docker build -f ${DIR}/Dockerfile \
    --build-arg JAR_FOLDER=${FOLDER}/build/libs \
    -t ${NAME}:${VERSION} .
}


cd $DIR
cd ..
# Building the app:
echo "Building JAR files"
./gradlew bootJar -x test

echo "Unpacking JARs"
unpack .

echo "Building Docker image"
build . pie-tunes-streaming

cd $USER_DIR
