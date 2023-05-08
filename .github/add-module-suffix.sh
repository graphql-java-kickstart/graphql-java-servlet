#!/bin/bash

MODULE_SUFFIX="${GITHUB_REF##*/}"

addSuffix() {
  local result
  result=$(grep include settings.gradle | awk '{print $2}' | tr -d "'" | tr -d ':')
  readarray -t <<<"$result"
  modules=("${MAPFILE[@]}")

  replaceLocalDependencies
}

updateLocalDependencies() {
  for module in "${modules[@]}"; do
    echo "$module"
    cp -rf "$module" "$module"-"$MODULE_SUFFIX"

    for dependency in "${modules[@]}"; do
      sed -i -E "s/project\(('|\"):${dependency}('|\")\)/project\(':${dependency}-${MODULE_SUFFIX}'\)/" "$module"-"$MODULE_SUFFIX"/build.gradle
    done
  done
}

echo "Add suffix '-$MODULE_SUFFIX' to modules"
#addSuffix
