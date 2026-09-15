# LocalCam 📱💻

Turn your Android phone into an ultra-low-latency wireless PC webcam and OBS Studio broadcast camera over your local Wi-Fi or phone portable hotspot. **100% offline — zero internet or cellular data required.**

Compatible with **Zoom, Google Meet, Microsoft Teams, Discord, Skype, OBS Studio, and all modern web browsers**.

---

## ⚡ Quick Start: 3-Step Setup

```
┌─────────────────┐       Wi-Fi Hotspot / LAN        ┌──────────────────┐
│  Android Phone  │ ───────────────────────────────> │  Laptop / PC     │
│   (LocalCam)    │     http://192.168.43.1:8080     │ (Zoom/Meet/OBS)  │
└─────────────────┘                                  └──────────────────┘
```

### Step 1: Set Up the Phone
1. Open **LocalCam** on your Android device.
2. Grant Camera permission when prompted.
3. Choose your network mode:
   - **Recommended (100% Offline):** Turn ON your phone's **Personal Hotspot** in Android Settings.
     > 💡 **Note:** Mobile Data / Cellular Data can stay **OFF**. The phone hotspot creates a direct high-speed local network between the phone and PC without using any cellular data.
   - **Alternative:** Connect both your phone and PC to the same home/office Wi-Fi router.
4. Tap **"START STREAM"** in LocalCam. The app displays your local stream URL (e.g., `http://192.168.43.1:8080`).

---

### Step 2: Choose How to Use on Your PC

#### Option A: PC Virtual Webcam (Zoom, Meet, Teams, Discord, Skype)
Use the included **LocalCam PC Virtual Webcam Client** to feed video directly into conference apps as a native camera device:

1. Connect your laptop to your phone's Wi-Fi Hotspot (or same Wi-Fi).
2. Open your laptop browser and navigate to:
   ```
   http://192.168.43.1:8080
   ```
3. Click **"Download Windows Launcher (.bat)"** (or Mac/Linux script).
   *(You can also use the files in the `/pc-client` directory of this repo).*
4. Run the launcher:
   - **Windows:** Double-click `start_pc_client.bat`.
   - **macOS / Linux:** Run `./start_pc_client.sh`.
5. In the PC client window, verify the IP (`192.168.43.1`) and click **"Start Webcam Feed"**.
6. Open **Zoom, Teams, Google Meet, or Discord**, go to Video Settings, and select:
   - **Windows/Mac:** **"OBS Virtual Camera"** or **"LocalCam"**
   - **Linux:** **"LocalCam Virtual Webcam"** (`/dev/video10`)

---

#### Option B: OBS Studio (Streamers & Content Creators)
Add your phone camera directly into OBS scenes as a clean, high-framerate video source:

1. In OBS Studio, click the **`+`** icon under **Sources** and select **Browser**.
2. Name it (e.g., "Phone Webcam") and configure:
   - **URL:** `http://192.168.43.1:8080/obs` *(clean, edge-to-edge video)*
   - **Width:** `1280` (or `1920`)
   - **Height:** `720` (or `1080`)
   - Check **"Shutdown source when not visible"**
3. Alternatively, for direct hardware decoding:
   - Add **Media Source** -> uncheck **Local File** -> set Input to `http://192.168.43.1:8080/video`.

---

#### Option C: Web Browser Receiver & Remote Controls
Access full live video and remote camera controls from any browser on your network:

1. Open Chrome, Edge, Safari, or Firefox on any laptop, tablet, or phone on the network.
2. Go to `http://192.168.43.1:8080`
3. Enjoy live streaming and remote controls:
   - ⚡ Toggle phone torch / flashlight remotely
   - 🔄 Flip between Rear and Front cameras
   - 🔍 Control digital zoom
   - 📸 Trigger remote high-resolution photos saved directly to phone storage
   - 💾 Download still snapshots directly to PC

---

## 🛠 Virtual Camera Driver Prerequisites

The desktop client uses `pyvirtualcam` to pipe video into your operating system's virtual camera layer:

| Platform | Recommended Virtual Camera Driver | Instructions |
| :--- | :--- | :--- |
| **Windows** | **OBS Virtual Camera** (Included with OBS Studio) | Install [OBS Studio](https://obsproject.com/). Click "Start Virtual Camera" once to register DirectShow filter. Alternatively, install [Unity Capture](https://github.com/schellingb/UnityCapture). |
| **macOS** | **OBS macOS Virtual Camera** | Install OBS Studio or the OBS Virtual Camera plugin. |
| **Linux** | **v4l2loopback** | Run: `sudo apt install v4l2loopback-dkms` <br> `sudo modprobe v4l2loopback devices=1 video_nr=10 card_label="LocalCam Virtual Webcam" exclusive_caps=1` |

---

## 📱 Phone App Settings & Customization

Tap the **Settings (gear)** icon on the phone to configure:
- **Resolution:** 720p HD (fastest, lowest latency), 1080p Full HD (maximum clarity), or 480p SD (battery saver).
- **Target Frame Rate:** 30 FPS or 24 FPS broadcast standard.
- **JPEG Compression Quality:** 50% to 95% quality slider.
- **Keep Screen Awake:** Prevents phone display from sleeping so streaming remains uninterrupted.
- **Stream Port:** Default `8080` (customizable).

---

## ❓ Troubleshooting & Frequently Asked Questions

### 1. The laptop browser says "Site cannot be reached"
- **Check connection:** Make sure your laptop is connected to your phone's hotspot or the exact same Wi-Fi SSID.
- **Verify IP address:** Ensure you are entering the exact IP shown in the LocalCam phone HUD (typically `192.168.43.1:8080` on hotspot).
- **Windows Firewall:** If prompted, allow Python and your browser through the Windows Private Network Firewall.

### 2. Can I use this completely without cellular data or SIM card?
**Yes!** Turn off Mobile Data in Android settings. The Wi-Fi hotspot functions as a local Area Network (LAN). Video streams directly from phone to laptop via Wi-Fi radio waves without touching the internet.

### 3. How do I achieve the lowest latency (<50ms)?
- Use the **Phone Hotspot** instead of an overcrowded home Wi-Fi router.
- Select **720p HD @ 30 FPS** with 75% JPEG quality in LocalCam Settings.
- Place the phone within 2–5 meters of your laptop.

### 4. How do I keep the phone cool during long conference calls?
- Lower phone screen brightness to minimum (the screen backlights produce most heat).
- Plug the phone into a charger or laptop USB port.
- Keep the phone out of direct sunlight or thick cases.
