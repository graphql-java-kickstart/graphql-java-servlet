#!/bin/bash
set -ev

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

echo "Publishing release to Maven Central"
removeSnapshots
.github/add-jakarta5-suffix.sh
./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository