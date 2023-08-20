#!/usr/bin/env bash

if [[ "$(pwd | awk -F/ '{print $NF}')" -ne "element-web" ]]; then
  echo "Not in element-web"
  exit 1
fi

#yarn clean || exit 1
cp ../config.element-web.json ./config.json
yarn install || exit 1
yarn build || exit 1

rsync -va --delete  ./webapp/ ../target/classes/static/element-web/

