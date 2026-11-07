@echo off
title LocalCam - Remove Virtual Camera
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    echo UAC.ShellExecute "cmd.exe", "/c """"%~s0""""", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B
)
set DEST=%ProgramData%\LocalCam\driver
regsvr32 /u /s "%DEST%\UnityCaptureFilter64.dll"
%SYSTEMROOT%\SysWOW64\regsvr32.exe /u /s "%DEST%\UnityCaptureFilter32.dll"
echo.
echo Virtual Camera driver has been removed.
pause
