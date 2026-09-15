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

### Windows (NO OBS STUDIO REQUIRED!)
You do **NOT** need OBS Studio installed. Windows simply requires registering a lightweight DirectShow virtual camera driver:

1. **One-Time Driver Setup (takes 2 seconds):**
   - Double-click or right-click `install_virtual_camera.bat` and select **"Run as administrator"**.
   - Click "Yes" on the Windows prompt.
   - It registers the DirectShow filter (`UnityCaptureFilter64.dll`).
2. **Start Streaming:**
   - On phone: Tap **START STREAM**.
   - On PC: Double-click `start_pc_client.bat` and click **"Start Webcam Feed"**.
3. **Select in VideoPsalm, Google Meet, Zoom, or Teams:**
   - Open your app's Video/Camera settings.
   - Select **"Unity Video Capture"** (or "LocalCam")!
   - Video appears immediately in HD with ultra-low latency.

*(Note: If you happen to already have OBS Studio installed, `pyvirtualcam` can also output to `OBS Virtual Camera` automatically).*

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

## 📱 Phone Setup & Offline Connection Notes

1. **Zero Mobile Data Needed:**
   - On your Android phone, turn ON **Personal Hotspot**.
   - You can keep Mobile / Cellular Data **OFF** — the direct Wi-Fi hotspot creates a local wireless link that requires no internet connection.
2. **Finding the Phone IP:**
   - When connected via Phone Hotspot, the phone's IP address is typically `192.168.43.1`.
   - If both phone and laptop are connected to a home/office router, LocalCam displays the exact IP address on the phone's camera HUD.
3. **Downloading the Client Directly from the Phone:**
   - Once connected to the phone's hotspot, you don't even need internet access to download this PC client!
   - Simply open `http://192.168.43.1:8080/pc` in your laptop browser to download `start_pc_client.bat`, `localcam_pc_client.py`, and `requirements.txt` directly from the phone.

---

## CLI / Headless Mode
If you prefer running without a GUI or inside a script:
```bash
python localcam_cli.py --ip 192.168.43.1 --port 8080
```
Optional flags:
- `--no-mirror`: Disable horizontal mirroring
- `--fps 30`: Target framerate
