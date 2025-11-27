@echo off
echo Cleaning build artifacts...

setlocal ENABLEDELAYEDEXPANSION

REM Define directories
set FOLDERS=build gen output

for %%F in (%FOLDERS%) do (
    if exist %%F (
        echo Deleting folder %%F...
        rmdir /s /q %%F
    ) else (
        echo Folder %%F does not exist, skipping...
    )
)

REM Clean temporary files
echo Deleting temporary files...

for /r %%f in (*.tokens) do (
    echo Deleting file %%f...
    del /f /q "%%f"
)

for /r %%f in (*.interp) do (
    echo Deleting file %%f...
    del /f /q "%%f"
)

echo Clean completed successfully.