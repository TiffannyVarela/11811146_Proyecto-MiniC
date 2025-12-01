@echo off
echo ========================================
   DIAGNOSTICO COMPLETO
echo ========================================

echo 1. Estructura de gen\:
dir gen /b

echo.
echo 2. Archivos en gen\:
if exist gen\*.java (
    for %%f in (gen\*.java) do (
        echo %%f:
        findstr "package" "%%f"
    )
)

echo.
echo 3. Verificando classpath...
echo Classpath actual: %CLASSPATH%

echo.
echo 4. Probando compilacion manual...
javac -cp "lib\antlr-4.13.2-complete.jar" -d bin gen\*.java

if errorlevel 1 (
    echo ERROR en compilacion ANTLR
) else (
    echo OK - ANTLR compilado
)

echo.
echo 5. Verificando bin\:
dir bin /b

pause