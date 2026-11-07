@echo off
title LocalCam - Virtual Camera Driver Setup (No OBS Required)
color 0B
echo ======================================================================
echo           LocalCam Windows Virtual Camera Installer (NO OBS!)
echo ======================================================================
echo.
echo This registers the lightweight DirectShow Virtual Camera driver into
echo Windows so that Zoom, Google Meet, Microsoft Teams, Discord, and
echo VideoPsalm detect LocalCam as a native webcam device.
echo.
echo No OBS Studio installation is required!
echo.

:: Check for administrative privileges
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo [INFO] Requesting Administrative Privileges to register the driver...
    goto UACPrompt
) else ( goto gotAdmin )

:UACPrompt
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    set params=%*
    echo UAC.ShellExecute "cmd.exe", "/c """"%~s0"" %params%", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B

:gotAdmin
    pushd "%CD%"
    CD /D "%~dp0"

    set DRIVER_DIR=%~dp0driver
    if not exist "%DRIVER_DIR%\UnityCaptureFilter64.dll" (
        set DRIVER_DIR=%~dp0
    )

    if not exist "%DRIVER_DIR%\UnityCaptureFilter64.dll" (
        echo [INFO] Driver DLLs not found locally. Downloading from GitHub...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/schellingb/UnityCapture/archive/refs/heads/master.zip' -OutFile '%temp%\unitycapture.zip'"
        powershell -Command "Expand-Archive -Path '%temp%\unitycapture.zip' -DestinationPath '%temp%\uc_extract' -Force"
        mkdir "%~dp0driver" 2>nul
        copy "%temp%\uc_extract\UnityCapture-master\Install\UnityCaptureFilter*.dll" "%~dp0driver\" >nul
        set DRIVER_DIR=%~dp0driver
    )

    if not exist "%DRIVER_DIR%\UnityCaptureFilter64.dll" (
        echo [ERROR] Could not locate or download UnityCaptureFilter64.dll.
        echo Please ensure you are connected to the phone or internet.
        pause
        exit /B 1
    )

    echo [1/3] Clearing any existing filter instances...
    regsvr32 /u /s "%DRIVER_DIR%\UnityCaptureFilter64.dll" 2>nul
    regsvr32 /u /s "%DRIVER_DIR%\UnityCaptureFilter32.dll" 2>nul

    echo [2/3] Registering 64-bit Virtual Camera Filter...
    regsvr32 /s "%DRIVER_DIR%\UnityCaptureFilter64.dll"

    echo [3/3] Registering 32-bit Virtual Camera Filter...
    regsvr32 /s "%DRIVER_DIR%\UnityCaptureFilter32.dll"

    echo.
    echo ======================================================================
    echo   SUCCESS! Virtual Camera registered successfully!
    echo ======================================================================
    echo.
    echo VideoPsalm / Google Meet / Zoom Setup Instructions:
    echo 1. Close any browser tabs with Google Meet, Zoom, Teams, or Camera apps.
    echo 2. Run start_pc_client.bat and click "START WEBCAM FEED" FIRST.
    echo 3. In VideoPsalm: Go to Settings ^> Video and select "Unity Video Capture".
    echo.
    echo (If VideoPsalm ever says "Webcam not available", click "Reset Virtual Cam"
    echo  in the LocalCam PC Client and make sure other camera apps are closed.)
    echo ======================================================================
    echo.
    pause
