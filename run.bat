@echo off
echo ================================
echo    MINIC COMPILER
echo ================================
echo.

REM Configurar el classpath
set CLASSPATH=build\minic.jar;lib\antlr-4.13.2-complete.jar

REM Verificar que existe el JAR
if not exist "build\minic.jar" (
    echo Error: No se encontró build\minic.jar
    echo Ejecuta primero: build.bat
    pause
    exit /b 1
)

REM Ejecutar el compilador
if "%1"=="" (
    java -cp "%CLASSPATH%" org.minic.MiniCCompiler --help
) else (
    echo Archivo de entrada: %1
    echo.
    java -cp "%CLASSPATH%" org.minic.MiniCCompiler %*
)

REM Pausar si hay error
if errorlevel 1 (
    echo.
    echo Presiona cualquier tecla para salir...
    pause > nul
)