@echo off
echo Building the project...

REM Add your build commands here
set ANTLR_JAR=lib\antlr-4.13.2-complete.jar
set GEN=gen
set SRC=src\main\java
set BUILD=build

REM Clean previous build
echo Eliminando carpeta de build anterior...

if exist %GEN% (
    
    rmdir /s /q %GEN%
)

if exist %BUILD% (
    rmdir /s /q %BUILD%
)

mkdir %GEN%
mkdir %BUILD%

REM Generate parser and lexer from grammar files
echo Generating parser and lexer...
java -jar %ANTLR_JAR% ^
    -Dlanguage=Java ^
    -visitor ^
    -listener ^
    -o %GEN% ^
    grammar\MiniCLexer.g4 grammar\MiniC.g4

IF %ERRORLEVEL% NEQ 0 (
    echo Error al generar el parser y lexer.
    exit /b %ERRORLEVEL%
)

REM Compile Java source files
echo Compiling Java source files...

set CLASSPATH=%ANTLR_JAR%

REM Compilar archivos generados
javac -cp %CLASSPATH% -d %BUILD% %GEN%\org\minic\*.java

IF %ERRORLEVEL% NEQ 0 (
    echo Error al compilar los archivos Java.
    exit /b %ERRORLEVEL%
)

REM Compilar archivos fuente
echo Compiling source files...
for /r %SRC% %%f in (*.java) do (
    javac -cp %CLASSPATH% -d %BUILD% "%%f"
)

IF %ERRORLEVEL% NEQ 0 (
    echo Error al compilar los archivos fuente.
    exit /b %ERRORLEVEL%
)

echo Build completed successfully.