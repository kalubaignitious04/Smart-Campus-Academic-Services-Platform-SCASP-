#!/bin/sh
# Compiles every module's src/main into out/classes (Java 21 JDK required).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out/classes out/test
javac --release 21 -d out/classes -cp "lib/*" $(find core-domain concurrent-processing network-gateway persistence-jdbc rmi-administration web-application -path "*/src/main/*" -name "*.java")
mkdir -p out/classes/database && cp database/*.sql out/classes/database/
echo "Build OK: out/classes"
