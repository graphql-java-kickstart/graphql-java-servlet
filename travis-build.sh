#!/bin/bash
set -ev

if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ] && [ "${RELEASE}" = "true" ]; then
    echo "Deploying release to Bintray"
    git checkout -f ${TRAVIS_BRANCH}
    ./gradlew clean assemble release -Prelease.useAutomaticVersion=true && ./gradlew check --info
fi
