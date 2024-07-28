#!/bin/bash
set -ev

FLAVOUR="${1}"

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

echo "Publishing release to Maven Central"
removeSnapshots

if [ "${FLAVOUR}" == 'javax' ]; then
  .github/add-javax-suffix.sh
fi

./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepositories
