@echo off
cd /d "%~dp0.."
rem pyvirtualcam/opencv only ship wheels for Python 3.8-3.13, so pick a compatible one (not 3.14/3.15)
set PYX=
for %%V in (3.12 3.11 3.13 3.10) do (
    if not defined PYX (
        py -%%V -c "import sys" >nul 2>&1 && set PYX=py -%%V
    )
)
if not defined PYX (
    echo [ERROR] No compatible Python found. You have a Python that is too new for pyvirtualcam.
    echo Install Python 3.12 from https://www.python.org/downloads/release/python-3128/
    echo ^(tick "Add python.exe to PATH"^), then run this again. Or just download the ready-made
    echo LocalCam.exe from https://github.com/anointedthedeveloper/LocalCam/releases/tag/latest
    pause
    exit /b 1
)
echo Using: %PYX%
%PYX% -m venv .venv-build || exit /b 1
call .venv-build\Scripts\activate.bat
python -m pip install -q -r pc-client\requirements.txt pyinstaller || exit /b 1
python -m PyInstaller --noconfirm --clean --onefile --windowed --name LocalCam --distpath dist --workpath build\pyinstaller --specpath build\pyinstaller --add-data "%CD%\pc-client\driver;driver" --add-data "%CD%\pc-client\install_virtual_camera.bat;." --add-data "%CD%\pc-client\uninstall_virtual_camera.bat;." --collect-all pyvirtualcam pc-client\localcam_pc_client.py || exit /b 1
echo.
echo Desktop executable: dist\LocalCam.exe
pause
