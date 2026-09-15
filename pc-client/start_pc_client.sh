#!/usr/bin/env bash
set -e

echo "========================================================"
echo "        LocalCam PC Virtual Webcam Client"
echo "========================================================"
echo ""

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if ! command -v python3 &> /dev/null; then
    echo "[ERROR] python3 could not be found."
    echo "Please install Python 3.8+ on your system."
    exit 1
fi

echo "[1/2] Installing requirements..."
python3 -m pip install -q -r "$SCRIPT_DIR/requirements.txt" || true

echo "[2/2] Launching LocalCam Desktop Client..."
python3 "$SCRIPT_DIR/localcam_pc_client.py"
