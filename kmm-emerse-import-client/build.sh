#!/bin/bash

set -e

case "$1" in

"")
  echo You must specify a build profile.
  echo For example: ./build.sh dev
  echo Recognized profiles are: dev, prod
  exit 1
;;

"prod")
  config="production"
  suffix=""
;;


"dev")
  config="$1"
  suffix="$1"
;;

*)
  echo Unrecognized build profile: $1
  exit 1
;;

esac

echo Generating $1 build...
echo Installing dependencies...
cd angular
npm install $2
npm run snapshot
npm run git-hash
echo Building distribution...
ng build --configuration=$config $3
echo Building war...
cd -
mvn clean package -Dbuild.profile="$1" -Dwar.suffix="$suffix" $4


