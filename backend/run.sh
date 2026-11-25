#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -encoding UTF-8 -d out src/com/campusmarketplace/*.java || exit 1
exec java -cp out com.campusmarketplace.Main "$@"
