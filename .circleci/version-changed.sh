#!/bin/bash

if [ $# -ne 2 ]; then
  echo "You must provide the libraryName argument"
  exit 1
else
  libraryDirectory=$1
  libraryName=$2

  libraryVersion=$(grep '^version=.*$' "${libraryDirectory}/gradle.properties" | sed "s/^version=\(.*\)$/\1/")

  echo "Checking ${libraryName}:${libraryVersion} in artifactory."

  artifactPath="https://arti.tw.ee/artifactory/libs-release-local/com/transferwise/common/${libraryName}/${libraryVersion}/${libraryName}-${libraryVersion}.jar"
  echo "${artifactPath}"
  artifactStatus=$(curl -s -o /dev/null -I -w "%{http_code}" "${artifactPath}")

  if [ "${artifactStatus}" == "404" ]; then
    echo "${libraryName} version ${libraryVersion} does not exist. Publishing."
    exit 0
  else
    echo "${libraryName} version ${libraryVersion} exists. Skip publishing."
    exit 1
  fi
fi