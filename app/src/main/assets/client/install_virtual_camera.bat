@echo off
title LocalCam - Virtual Camera Driver Setup (No OBS Required)
color 0B
echo ======================================================================
echo           LocalCam Windows Virtual Camera Installer (NO OBS!)
echo ======================================================================
echo.

>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo [INFO] Requesting Administrator rights to register the driver...
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    echo UAC.ShellExecute "cmd.exe", "/c """"%~s0""""", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B
)

CD /D "%~dp0"
set SRC=%~dp0driver
if not exist "%SRC%\UnityCaptureFilter64.dll" set SRC=%~dp0
set DEST=%ProgramData%\LocalCam\driver
set LOGF=%ProgramData%\LocalCam\install.log
set CLSID64={5C2CD55C-92AD-4999-8666-912BD3E70010}
set CLSID32={5C2CD55C-92AD-4999-8666-912BD3E70020}

if not exist "%SRC%\UnityCaptureFilter64.dll" (
    echo [ERROR] UnityCaptureFilter64.dll not found next to this script.
    echo Re-download the LocalCam client folder and try again.
    pause
    exit /B 1
)

mkdir "%ProgramData%\LocalCam" 2>nul
echo [%date% %time%] install start, src=%SRC% > "%LOGF%"

echo [1/4] Copying driver to a permanent folder: %DEST%
echo       (Windows keeps pointing at this path, so it must not be a temp folder)
mkdir "%DEST%" 2>nul
copy /y "%SRC%\UnityCaptureFilter64.dll" "%DEST%\" >nul
copy /y "%SRC%\UnityCaptureFilter32.dll" "%DEST%\" >nul

echo [2/4] Registering 64-bit filter...
regsvr32 /s "%DEST%\UnityCaptureFilter64.dll"
set R64=%errorlevel%
echo [%date% %time%] regsvr32 64-bit exit=%R64% >> "%LOGF%"

echo [3/4] Registering 32-bit filter (needed by 32-bit apps such as VideoPsalm)...
%SYSTEMROOT%\SysWOW64\regsvr32.exe /s "%DEST%\UnityCaptureFilter32.dll"
set R32=%errorlevel%
echo [%date% %time%] regsvr32 32-bit exit=%R32% >> "%LOGF%"

echo [4/4] Verifying...
set OK64=NO
set OK32=NO
reg query "HKLM\SOFTWARE\Classes\CLSID\%CLSID64%" >nul 2>&1 && set OK64=YES
if "%R64%"=="0" set OK64=YES
reg query "HKLM\SOFTWARE\Classes\WOW6432Node\CLSID\%CLSID32%" >nul 2>&1 && set OK32=YES
if "%R32%"=="0" set OK32=YES
echo [%date% %time%] verify 64=%OK64% 32=%OK32% >> "%LOGF%"
echo       64-bit registered: %OK64%  ^(regsvr32 exit code %R64%^)
echo       32-bit registered: %OK32%  ^(regsvr32 exit code %R32%^)
echo.

if "%OK64%%OK32%"=="YESYES" (
    echo ======================================================================
    echo   SUCCESS! "Unity Video Capture" is now a camera on this PC.
    echo ======================================================================
    echo 1. Start LocalCam and click START WEBCAM FEED.
    echo 2. RESTART VideoPsalm / Zoom / Meet ^(apps only scan cameras at startup^).
    echo 3. Choose "Unity Video Capture" as the video source.
) else (
    color 0C
    echo ======================================================================
    echo   FAILED - the camera driver did not register. Details: %LOGF%
    echo ======================================================================
    echo Try: right-click this file ^> Run as administrator, and temporarily
    echo pause antivirus if it blocks the DLL. Send the log above if it persists.
)
echo.
pause
