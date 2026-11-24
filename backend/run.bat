@echo off
REM Compiles the backend into out\ and starts it on port 8080 (pass another port as an argument).
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -encoding UTF-8 -d out -sourcepath src src\com\campusmarketplace\Main.java
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)
java -cp out com.campusmarketplace.Main %*
