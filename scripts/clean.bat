@echo off
echo Limpiando proyecto MiniC...
if exist bin rmdir /s /q bin
if exist gen rmdir /s /q gen
if exist build rmdir /s /q build
if exist out rmdir /s /q out
echo Proyecto limpiado.