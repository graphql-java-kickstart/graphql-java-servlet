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
    cp -rf "$module" "$module"-javax
    rm -rf "$module"

    for dependency in "${modules[@]}"; do
      sed -i -E "s/project\(('|\"):${dependency}('|\")\)/project\(':${dependency}-javax'\)/" "$module"-"javax"/build.gradle
    done
  done

  updateGradleSettings
}

updateGradleSettings() {
  for module in "${modules[@]}"; do
    echo "Replace ${module} with ${module}-javax in settings.gradle"
    sed -i -E "s/('|\"):${module}('|\")/':${module}-javax'/" settings.gradle
  done

  cat settings.gradle
}

echo "Add suffix -javax to modules"
addSuffix

ls -lh