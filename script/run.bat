@echo off
SETLOCAL EnableExtensions

set "JAR_PATH=build\libs\AnkiMediaVolume.jar"

if not exist "%JAR_PATH%" (
    .\gradlew shadowJar
)

java -jar "%JAR_PATH%"

endlocal