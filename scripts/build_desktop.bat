@echo off
cd /d "%~dp0.."
python -m pip install -q -r pc-client\requirements.txt pyinstaller || exit /b 1
python -m PyInstaller --noconfirm --clean --onefile --windowed --name LocalCam --distpath dist --workpath build\pyinstaller --specpath build\pyinstaller --add-data "%CD%\pc-client\driver;driver" --add-data "%CD%\pc-client\install_virtual_camera.bat;." --add-data "%CD%\pc-client\uninstall_virtual_camera.bat;." --collect-all pyvirtualcam pc-client\localcam_pc_client.py || exit /b 1
echo Desktop executable: dist\LocalCam.exe
