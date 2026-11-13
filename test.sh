#!/bin/sh
# Builds, compiles the tests and runs all of them. Output also saved to docs/testing/test-report.txt
set -e
cd "$(dirname "$0")"
./build.sh
javac --release 21 -d out/test -cp "lib/*:out/classes" $(find core-domain concurrent-processing network-gateway persistence-jdbc rmi-administration web-application -path "*/src/test/*" -name "*.java")
java -cp "lib/*:out/classes:out/test" org.junit.platform.console.ConsoleLauncher --scan-classpath out/test --details=tree --disable-banner 2>&1 | tee docs/testing/test-report.txt
