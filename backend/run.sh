#!/usr/bin/env sh
# Compiles the backend into out/ and starts it on port 8080 (pass another port as an argument).
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -encoding UTF-8 -d out -sourcepath src src/com/campusmarketplace/Main.java || exit 1
exec java -cp out com.campusmarketplace.Main "$@"
