@echo off
REM Compile and run Campus Marketplace console app
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out src\com\campusmarketplace\*.java
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)
java -cp out com.campusmarketplace.Main %*
