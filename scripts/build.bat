@echo off
REM Automatizacion para compilacion del proyecto:
REM Incluye:
REM Limpieza de directorios
REM Generacion de codigo ANTLR
REM Insercion del paquete org.minic en los archivos ANTLR
REM Compilacion de todas las fases (parser, AST, semantico, IR, backend MIPS, optimizador)
REM Construccion del JAR ejecutable con MANIFEST

REM USO: scripts\build.bat

REM TRAS LA COMPILACION:
REM java -cp "build\minic.jar;lib\antlr-4.13.2-complete.jar" org.minic.MiniCCompiler --help

echo    COMPILADOR MINIC

REM LIMPIEZA DE DIRECTORIOS
echo Limpiando...
if exist gen rmdir /s /q gen
if exist bin rmdir /s /q bin
if exist build rmdir /s /q build

REM CREACION DE DIRECTORIOS
echo Creando directorios...
mkdir gen
mkdir bin
mkdir build

REM GENERACION DE GRAMATICA ANTLR
echo Generando gramatica...
java -cp "lib\antlr-4.13.2-complete.jar" org.antlr.v4.Tool -Dlanguage=Java -o gen grammar\MiniC.g4

REM VERIFICACION Y AJUSTES DE LA ESTRUCTURA
if exist gen\grammar (
    
    REM VERIFICAR SI LOS ARCHIVOS ANTLS YA TIENEN 'package'
    findstr "package" gen\grammar\*.java >nul
    if errorlevel 1 (
        echo No se encontro package - agregando package org.minic a archivos...
        REM AGREGAR package org.minic
        for %%f in (gen\grammar\*.java) do (
            powershell -Command "(gc '%%f') -replace '^(@header {)', '$$1\npackage org.minic;' | Out-File -Encoding UTF8 '%%f'"
        )
    )
    REM MOVER ARCHIVOS GENERADOS A LA RUTA CORRECTA
    if not exist gen\org\minic mkdir gen\org\minic
    move gen\grammar\*.java gen\org\minic\ >nul 2>&1
    if exist gen\grammar rmdir gen\grammar
)

echo 1. Compilando archivos ANTLR...
javac -cp "lib\antlr-4.13.2-complete.jar" -d bin gen\org\minic\*.java

echo 2. Compilando paquete principal...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\*.java

echo 3. Compilando AST...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\ast\*.java

echo 4. Compilando Semantic...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\semantic\*.java

echo 5. Compilando IR...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\ir\*.java

echo 6. Compilando Backend MIPS...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\backend\mips\*.java

echo 7. Compilando Optimizer...
javac -cp "bin;lib\antlr-4.13.2-complete.jar" -d bin src\main\java\org\minic\optimizer\*.java

REM CREACION DEL ARCHIVO MANIFEST
echo Creando MANIFEST.MF...
(
echo Manifest-Version: 1.0
echo Main-Class: org.minic.MiniCCompiler
echo Class-Path: lib/antlr-4.13.2-complete.jar
) > MANIFEST.MF

REM CREACION DEL ARCHIVO JAR
jar cfm build\minic.jar MANIFEST.MF -C bin .

echo.
echo BUILD EXITOSO!
echo.
echo PARA USAR EL COMPILADOR:
echo run.bat --help