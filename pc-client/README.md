# LocalCam PC Virtual Webcam Client

Turn your Android phone into a high-performance wireless PC webcam over your local Wi-Fi or phone hotspot. Works with **Zoom, Google Meet, Microsoft Teams, Discord, Skype, OBS Studio, and web browsers**.

---

## Features
- **Zero Internet Required:** Streams directly across local Wi-Fi or phone portable hotspot.
- **Hardware Virtual Camera:** Exposes the phone video stream as a native DirectShow / CoreMedia / v4l2 camera device on your PC.
- **Sub-100ms Latency:** High-speed MJPEG stream with adaptive frame buffering.
- **Full Remote Phone Controls:**
  - Toggle Phone Flashlight/Torch
  - Flip Camera Lens (Back / Front)
  - Zoom In / Zoom Reset
  - Remote Shutter (Capture full-resolution photo to phone storage)
- **Selfie Mirroring:** Toggle horizontal flip for natural webcam appearance in conference calls.
- **Real-Time Telemetry:** Live FPS, resolution, phone battery/status monitor.

---

## Quick Start (1-Minute Setup)

### 1. Requirements
- **Python 3.8+** installed on your PC/Laptop ([python.org](https://www.python.org/downloads/)).
  *(During installation on Windows, ensure **"Add python.exe to PATH"** is checked).*

### 2. Launching the App

#### On Windows:
Double-click `start_pc_client.bat`. It will automatically install requirements and open the client.

#### On macOS / Linux:
Make the script executable and run:
```bash
chmod +x start_pc_client.sh
./start_pc_client.sh
```

#### Manual / Cross-Platform:
```bash
cd pc-client
pip install -r requirements.txt
python localcam_pc_client.py
```

---

## Virtual Camera Setup (By Operating System)

`LocalCam` uses `pyvirtualcam` to feed video into the virtual camera driver:

### Windows
1. If you have **OBS Studio** installed: simply click **"Start Virtual Camera"** once in OBS (or install OBS Virtual Camera). `pyvirtualcam` will automatically detect and stream to **"OBS Virtual Camera"**.
2. Alternatively, install **Unity Capture** or **Akvcam** DirectShow filters.
3. In **Zoom / Teams / Meet / Discord**, select **"OBS Virtual Camera"** or **"LocalCam"** in the camera dropdown!

### macOS
1. Install OBS Studio or the OBS macOS Virtual Camera plugin.
2. In Zoom / Teams / Meet, select **"OBS Virtual Camera"**.

### Linux (Ubuntu, Debian, Fedora, Arch)
Install `v4l2loopback`:
```bash
sudo apt install v4l2loopback-dkms
sudo modprobe v4l2loopback devices=1 video_nr=10 card_label="LocalCam Virtual Webcam" exclusive_caps=1
```
The client will automatically bind to `/dev/video10` and display as "LocalCam Virtual Webcam".

---

## CLI / Headless Mode
If you prefer running without a GUI or inside a script:
```bash
python localcam_cli.py --ip 192.168.43.1 --port 8080
```
Optional flags:
- `--no-mirror`: Disable horizontal mirroring
- `--fps 30`: Target framerate
