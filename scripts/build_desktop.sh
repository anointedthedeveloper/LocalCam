#!/usr/bin/env bash
# Builds a self-contained desktop executable (no Python / internet needed to run it) into dist/.
set -e
cd "$(dirname "$0")/.."
PY=python3; [ -x .venv/bin/python ] && PY=.venv/bin/python
$PY -m pip install -q -r pc-client/requirements.txt pyinstaller
# PyInstaller resolves paths relative to the spec dir and needs native absolute paths + OS-specific separator
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
ROOT=$($PY -c 'import os;print(os.path.abspath("."))')
SRC="$ROOT/pc-client"
echo "BUILD = \"${LOCALCAM_BUILD:-dev}\"" > "$SRC/_version.py"
$PY -m PyInstaller --noconfirm --clean --onefile --windowed --name LocalCam \
  --distpath "$ROOT/dist" --workpath "$ROOT/build/pyinstaller" --specpath "$ROOT/build/pyinstaller" \
  --add-data "$SRC/driver${SEP}driver" \
  --add-data "$SRC/install_virtual_camera.bat${SEP}." \
  --add-data "$SRC/uninstall_virtual_camera.bat${SEP}." \
  --collect-all pyvirtualcam \
  "$SRC/localcam_pc_client.py"
echo "Desktop executable -> dist/"; ls dist
