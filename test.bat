@echo off
cd /d "%~dp0"
call build.bat || exit /b 1
dir /s /b core-domain\src\test\*.java concurrent-processing\src\test\*.java network-gateway\src\test\*.java persistence-jdbc\src\test\*.java rmi-administration\src\test\*.java web-application\src\test\*.java > out\tests.txt
javac --release 21 -d out\test -cp "lib\*;out\classes" @out\tests.txt || exit /b 1
java -cp "lib\*;out\classes;out\test" org.junit.platform.console.ConsoleLauncher --scan-classpath out\test --details=tree --disable-banner
