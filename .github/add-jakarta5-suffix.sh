#!/bin/bash

addSuffix() {
  local result
  result=$(grep include settings.gradle | awk '{print $2}' | tr -d "'" | tr -d ':')
  readarray -t <<<"$result"
  modules=("${MAPFILE[@]}")

  updateLocalDependencies
}

updateLocalDependencies() {
  for module in "${modules[@]}"; do
    cp -rf "$module" "$module"-jakarta5
    rm -rf "$module"

    for dependency in "${modules[@]}"; do
      sed -i -E "s/project\(('|\"):${dependency}('|\")\)/project\(':${dependency}-jakarta5'\)/" "$module"-"jakarta5"/build.gradle
    done
  done

  updateGradleSettings
}

updateGradleSettings() {
  for module in "${modules[@]}"; do
    echo "Replace ${module} with ${module}-jakarta5 in settings.gradle"
    sed -i -E "s/('|\"):${module}('|\")/':${module}-jakarta5'/" settings.gradle
  done

  cat settings.gradle
}

echo "Add suffix -jakarta5 to modules"
addSuffix

ls -lh