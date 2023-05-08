#!/bin/bash
set -ev

BRANCH="${GITHUB_REF##*/}"

getVersion() {
  ./gradlew properties -q | grep -E "^version" | awk '{print $2}' | tr -d '[:space:]'
}

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

commitRelease() {
  local APP_VERSION
  if "${BRANCH}" == "master"; then
    APP_VERSION=$(getVersion)
  else
    APP_VERSION=$(getVersion)-"${BRANCH}"
  fi
  git commit -a -m "Update version for release"
  git tag -a "v${APP_VERSION}" -m "Tag release version"
}

bumpVersion() {
  echo "Bump version number"
  local APP_VERSION
  APP_VERSION=$(getVersion | xargs)
  local SEMANTIC_REGEX='^([0-9]+)\.([0-9]+)(\.([0-9]+))?$'
  if [[ ${APP_VERSION} =~ ${SEMANTIC_REGEX} ]]; then
    if [[ ${BASH_REMATCH[4]} ]]; then
      nextVersion=$((BASH_REMATCH[4] + 1))
      nextVersion="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${nextVersion}-SNAPSHOT"
    else
      nextVersion=$((BASH_REMATCH[2] + 1))
      nextVersion="${BASH_REMATCH[1]}.${nextVersion}-SNAPSHOT"
    fi

    echo "Next version: ${nextVersion}"
    sed -i -E "s/^version(\s)?=.*/version=${nextVersion}/" gradle.properties
    git commit -a -m "Bumped version for next release"
  else
    echo "No semantic version and therefore cannot publish to maven repository: '${APP_VERSION}'"
  fi
}

git config --global user.email "actions@github.com"
git config --global user.name "GitHub Actions"

echo "Deploying release to Maven Central"
removeSnapshots
commitRelease
bumpVersion
git push --follow-tags