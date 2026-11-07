#!/usr/bin/env bash
# Builds a self-contained desktop executable (no Python / internet needed to run it) into dist/.
set -e
cd "$(dirname "$0")/.."
PY=python3; [ -x .venv/bin/python ] && PY=.venv/bin/python
$PY -m pip install -q -r pc-client/requirements.txt pyinstaller
$PY -m PyInstaller --noconfirm --clean --onefile --windowed --name LocalCam \
  --distpath dist --workpath build/pyinstaller --specpath build/pyinstaller \
  --add-data "$PWD/pc-client/driver:driver" \
  --add-data "$PWD/pc-client/install_virtual_camera.bat:." \
  --add-data "$PWD/pc-client/uninstall_virtual_camera.bat:." \
  --collect-all pyvirtualcam \
  pc-client/localcam_pc_client.py
echo "Desktop executable -> dist/"; ls dist
