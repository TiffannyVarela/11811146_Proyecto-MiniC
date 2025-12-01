@echo off
echo    EJECUTANDO PRUEBAS DEL COMPILADOR

call build.bat

echo.
echo Ejecutando pruebas desde lista...
java -jar build\minic-compiler.jar tests\lista_pruebas.txt

echo.
echo Pruebas completadas.
pause