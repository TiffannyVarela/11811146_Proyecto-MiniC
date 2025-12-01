@echo off
echo ================================
echo    MINIC COMPILER
echo ================================
echo.

if "%1"=="" (
    java -cp "build\minic.jar;lib\antlr-4.13.2-complete.jar" org.minic.MiniCCompiler --help
) else (
    java -cp "build\minic.jar;lib\antlr-4.13.2-complete.jar" org.minic.MiniCCompiler %*
)