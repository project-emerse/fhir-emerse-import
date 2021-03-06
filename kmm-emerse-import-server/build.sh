#!/bin/bash

set -e

case "$1" in

"")
  echo You must specify a build profile.
  echo For example: ./build.sh dev
  echo Recognized profiles are: dev, tst
  exit 1
;;

"dev" | "tst")
;;

*)
  echo Unrecognized build profile: $1
  exit 1
;;

esac

echo Building war...
mvn clean package -Dbuild.profile="$1"


