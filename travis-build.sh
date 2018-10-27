#!/bin/bash
set -ev

saveGitCredentials() {
    cat >$HOME/.netrc <<EOL
machine github.com
login ${GITHUB_USERNAME}
password ${GITHUB_TOKEN}

machine api.github.com
login ${GITHUB_USERNAME}
password ${GITHUB_TOKEN}
EOL
    chmod 600 $HOME/.netrc
}

if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
    saveGitCredentials
    git checkout -f ${TRAVIS_BRANCH}
    if [ "${RELEASE}" = "true" ]; then
        echo "Deploying release to Bintray"
        ./gradlew clean assemble release -Prelease.useAutomaticVersion=true && ./gradlew check --info
#    else
#        echo "Deploying snapshot to Bintray"
#        ./gradlew artifactoryPublish && ./gradlew check --info
    fi
fi
