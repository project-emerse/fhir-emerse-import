#!/bin/bash

set -e

case "$1" in

"")
  echo You must specify a build profile.
  echo For example: ./build.sh dev
  echo Recognized profiles are: dev, prd
  exit 1
;;

"prd")
  config="production"
;;


"dev")
  config="$1"
;;

*)
  echo Unrecognized build profile: $1
  exit 1
;;

esac

echo Generating $1 build...
echo Installing dependencies...
cd angular
npm install @uukmm/npm-auto-snapshot
npm run clean-snapshots
npm install
npm run snapshot
npm run git-hash
echo Building distribution...
ng build --configuration=$config --base-href "./" --deploy-url "./"
echo Building war...
cd -
mvn clean package -Dbuild.profile="$1"


