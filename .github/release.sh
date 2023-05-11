#!/bin/bash
set -ev

FLAVOUR="${1}"

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

echo "Publishing release to Maven Central"
removeSnapshots

if [ -n "${FLAVOUR}" ]; then
  .github/add-module-suffix.sh $FLAVOUR
fi

./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository