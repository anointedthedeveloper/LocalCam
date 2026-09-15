@echo off
title LocalCam - Remove Virtual Camera Driver
color 0C
echo ======================================================================
echo           LocalCam Windows Virtual Camera Uninstaller
echo ======================================================================
echo.

>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo Requesting administrative privileges...
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    set params=%*
    echo UAC.ShellExecute "cmd.exe", "/c """"%~s0"" %params%", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B
)

pushd "%CD%"
CD /D "%~dp0"
set DRIVER_DIR=%~dp0driver
if not exist "%DRIVER_DIR%\UnityCaptureFilter64.dll" set DRIVER_DIR=%~dp0

regsvr32 /u /s "%DRIVER_DIR%\UnityCaptureFilter64.dll"
regsvr32 /u /s "%DRIVER_DIR%\UnityCaptureFilter32.dll"

echo.
echo Virtual Camera driver has been uninstalled.
echo.
pause
