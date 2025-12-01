@echo off
echo ========================================
echo    COMPILADOR MINIC - BUILD CORREGIDO
echo ========================================

echo Limpiando completamente...
if exist gen rmdir /s /q gen
if exist bin rmdir /s /q bin
if exist build rmdir /s /q build

echo Creando directorios...
mkdir gen
mkdir bin
mkdir build

echo Generando gramatica...
java -cp "lib\antlr-4.13.2-complete.jar" org.antlr.v4.Tool -Dlanguage=Java -o gen grammar\MiniC.g4

echo Verificando estructura generada...
if exist gen\grammar (
    echo Manejando archivos en gen\grammar\...
    
    echo Buscando package declaration en archivos ANTLR...
    findstr "package" gen\grammar\*.java >nul
    if errorlevel 1 (
        echo No se encontro package - agregando package org.minic a archivos...
        for %%f in (gen\grammar\*.java) do (
            echo Modificando %%~nxf
            powershell -Command "(gc '%%f') -replace '^(@header {)', '$$1\npackage org.minic;' | Out-File -Encoding UTF8 '%%f'"
        )
    )
    
    echo Moviendo archivos a estructura de paquete...
    if not exist gen\org\minic mkdir gen\org\minic
    move gen\grammar\*.java gen\org\minic\ >nul 2>&1
    if exist gen\grammar rmdir gen\grammar
)

echo Compilando paso a paso...
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

echo Creando MANIFEST.MF...
(
echo Manifest-Version: 1.0
echo Main-Class: org.minic.MiniCCompiler
echo Class-Path: lib/antlr-4.13.2-complete.jar
) > MANIFEST.MF

echo Creando JAR...
jar cfm build\minic.jar MANIFEST.MF -C bin .

echo VERIFICACION:
echo Archivos en bin\org\minic\:
dir bin\org\minic /b

echo.
echo BUILD EXITOSO!
echo.
echo PARA USAR EL COMPILADOR:
echo java -cp "build\minic.jar;lib\antlr-4.13.2-complete.jar" org.minic.MiniCCompiler --help