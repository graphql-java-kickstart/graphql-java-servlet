#!/bin/bash
set -ev

BRANCH="${GITHUB_REF##*/}"

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

echo "Publishing release to Maven Central"
removeSnapshots

if [[ "${BRANCH}" != "master" ]]; then
  .github/add-module-suffix.sh
fi

./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository