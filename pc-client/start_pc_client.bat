@echo off
title LocalCam PC Virtual Webcam Client
color 0B
echo ========================================================
echo         LocalCam PC Virtual Webcam Client
echo ========================================================
echo.

where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not added to PATH.
    echo Please install Python 3.8+ from https://www.python.org/
    echo Make sure to check "Add Python to PATH" during installation.
    echo.
    pause
    exit /b
)

echo [1/2] Checking and installing dependencies...
python -m pip install -q -r "%~dp0requirements.txt"
if %errorlevel% neq 0 (
    echo [WARNING] Could not install some dependencies automatically.
    echo Attempting to launch anyway...
)

echo [2/2] Launching LocalCam Desktop Client...
echo.
python "%~dp0localcam_pc_client.py"

if %errorlevel% neq 0 (
    echo.
    echo An error occurred while running LocalCam.
    pause
)
