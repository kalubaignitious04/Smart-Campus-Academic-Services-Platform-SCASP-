@echo off
rem Compiles every module's src\main into out\classes (JDK 21 required)
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out\classes out\test
dir /s /b core-domain\src\main\*.java concurrent-processing\src\main\*.java network-gateway\src\main\*.java persistence-jdbc\src\main\*.java rmi-administration\src\main\*.java web-application\src\main\*.java > out\sources.txt
javac --release 21 -d out\classes -cp "lib\*" @out\sources.txt || exit /b 1
mkdir out\classes\database
copy /y database\*.sql out\classes\database\ >nul
echo Build OK: out\classes
