@echo off
cd /d "%~dp0"
java -cp "build\minic.jar;lib\antlr-4.13.2-complete.jar" org.minic.MiniCCompiler %*