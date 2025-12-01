@echo off
echo Verificando build...
echo.
echo Archivos en gen\org\minic\grammar\
dir /b gen\org\minic\grammar\*.java
echo.
echo Archivos en bin\org\minic\
dir /b bin\org\minic\*.class
echo.
echo Archivos en bin\org\minic\ast\
dir /b bin\org\minic\ast\*.class
echo.
echo Archivos en bin\org\minic\backend\mips\
dir /b bin\org\minic\backend\mips\*.class
echo.
echo Archivos en bin\org\minic\semantic\
dir /b bin\org\minic\semantic\*.class