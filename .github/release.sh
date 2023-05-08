#!/bin/bash
set -ev

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

echo "Publishing release to Maven Central"
removeSnapshots

./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository
