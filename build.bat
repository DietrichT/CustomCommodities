@echo off
rem Rebuilds jars/CustomCommodities.jar from src\.
rem Needs a JDK 17 (Starsector 0.98 runs on Java 17).
setlocal
cd /d "%~dp0"

set API=..\..\starsector-core\starfarer.api.jar
set LOG4J=..\..\starsector-core\log4j-1.2.9.jar
rem starfarer_obf.jar is needed because the commodity spec class and SpecStore are obfuscated:
rem a mod cannot use reflection (script sandbox), so the registration is a direct call.
set OBF=..\..\starsector-core\starfarer_obf.jar
set JSON=..\..\starsector-core\json.jar

if not exist out\classes mkdir out\classes
if not exist jars mkdir jars

dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 --release 17 -cp "%API%;%OBF%;%JSON%;%LOG4J%" -d out\classes @out\sources.txt
if errorlevel 1 goto :error

jar cf jars\CustomCommodities.jar -C out\classes .
if errorlevel 1 goto :error

echo Built jars\CustomCommodities.jar
goto :eof

:error
echo BUILD FAILED
exit /b 1
