#!/bin/bash

MODULE_SUFFIX="${GITHUB_REF##*/}"

addSuffix() {
  local result
  result=$(grep include settings.gradle | awk '{print $2}' | tr -d "'" | tr -d ':')
  readarray -t <<<"$result"
  modules=("${MAPFILE[@]}")

  updateLocalDependencies
}

updateLocalDependencies() {
  for module in "${modules[@]}"; do
    cp -rf "$module" "$module"-"$MODULE_SUFFIX"
    rm -rf "$module"

    for dependency in "${modules[@]}"; do
      sed -i -E "s/project\(('|\"):${dependency}('|\")\)/project\(':${dependency}-${MODULE_SUFFIX}'\)/" "$module"-"$MODULE_SUFFIX"/build.gradle
    done
  done

  updateGradleSettings
}

updateGradleSettings() {
  for module in "${modules[@]}"; do
    echo "Replace ${module} with ${module}-${MODULE_SUFFIX} in settings.gradle"
    sed -i -E "s/('|\"):${module}('|\")/':${module}-${MODULE_SUFFIX}'/" settings.gradle
  done

  cat settings.gradle
}

echo "Add suffix '-$MODULE_SUFFIX' to modules"
addSuffix

ls -lh