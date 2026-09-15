package com.example.server

object PcClientDistribution {

    const val REQUIREMENTS_TXT = """pyvirtualcam>=0.10.0
opencv-python>=4.8.0
Pillow>=9.0.0
requests>=2.28.0
numpy>=1.20.0
"""

    const val INSTALL_VIRTUAL_CAMERA_BAT = "@echo off\r\n" +
        "title LocalCam - Virtual Camera Driver Setup (No OBS Required)\r\n" +
        "color 0B\r\n" +
        "echo ======================================================================\r\n" +
        "echo           LocalCam Windows Virtual Camera Installer (NO OBS!)\r\n" +
        "echo ======================================================================\r\n" +
        "echo.\r\n" +
        "echo This registers the lightweight DirectShow Virtual Camera driver into\r\n" +
        "echo Windows so that VideoPsalm, Google Meet, Zoom, and Teams\r\n" +
        "echo detect LocalCam as a native webcam device.\r\n" +
        "echo.\r\n" +
        "cd /d \"%~dp0\"\r\n" +
        "set DRIVER_DIR=%~dp0driver\r\n" +
        "if not exist \"%DRIVER_DIR%\\UnityCaptureFilter64.dll\" set DRIVER_DIR=%~dp0\r\n" +
        "echo [1/3] Clearing any existing filter instances...\r\n" +
        "regsvr32 /u /s \"%DRIVER_DIR%\\UnityCaptureFilter64.dll\" 2>nul\r\n" +
        "regsvr32 /u /s \"%DRIVER_DIR%\\UnityCaptureFilter32.dll\" 2>nul\r\n" +
        "echo [2/3] Registering 64-bit Virtual Camera Filter...\r\n" +
        "regsvr32 /s \"%DRIVER_DIR%\\UnityCaptureFilter64.dll\"\r\n" +
        "echo [3/3] Registering 32-bit Virtual Camera Filter...\r\n" +
        "regsvr32 /s \"%DRIVER_DIR%\\UnityCaptureFilter32.dll\"\r\n" +
        "echo.\r\n" +
        "echo ======================================================================\r\n" +
        "echo   SUCCESS! Virtual Camera registered successfully!\r\n" +
        "echo ======================================================================\r\n" +
        "echo VideoPsalm / Google Meet / Zoom Setup:\r\n" +
        "echo 1. Close any browser tabs with Google Meet, Zoom, Teams, or Camera apps.\r\n" +
        "echo 2. Run start_pc_client.bat and click 'START WEBCAM FEED' FIRST.\r\n" +
        "echo 3. In VideoPsalm: Go to Settings ^> Video and select 'Unity Video Capture'.\r\n" +
        "echo ======================================================================\r\n" +
        "pause\r\n"

    const val UNINSTALL_VIRTUAL_CAMERA_BAT = "@echo off\r\n" +
        "title LocalCam - Remove Virtual Camera Driver\r\n" +
        "cd /d \"%~dp0\"\r\n" +
        "set DRIVER_DIR=%~dp0driver\r\n" +
        "if not exist \"%DRIVER_DIR%\\UnityCaptureFilter64.dll\" set DRIVER_DIR=%~dp0\r\n" +
        "regsvr32 /u /s \"%DRIVER_DIR%\\UnityCaptureFilter64.dll\"\r\n" +
        "regsvr32 /u /s \"%DRIVER_DIR%\\UnityCaptureFilter32.dll\"\r\n" +
        "echo Virtual Camera driver has been uninstalled.\r\n" +
        "pause\r\n"

    const val START_PC_CLIENT_BAT = """@echo off
title LocalCam PC Virtual Webcam Client
color 0B
echo ========================================================
echo         LocalCam PC Virtual Webcam Client
echo ========================================================
echo.

where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH.
    echo Please install Python 3.8+ from https://www.python.org/
    echo Make sure to check "Add Python to PATH" during installation.
    echo.
    pause
    exit /b
)

echo [1/2] Installing requirements...
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
"""

    val START_PC_CLIENT_SH: String = """#!/usr/bin/env bash
set -e
echo "========================================================"
echo "        LocalCam PC Virtual Webcam Client"
echo "========================================================"
echo ""

if ! command -v python3 &> /dev/null; then
    echo "[ERROR] python3 could not be found."
    echo "Please install Python 3.8+ on your system."
    exit 1
fi

echo "[1/2] Installing requirements..."
python3 -m pip install -q -r requirements.txt || true

echo "[2/2] Launching LocalCam Desktop Client..."
python3 localcam_pc_client.py
"""

    const val LOCALCAM_PC_CLIENT_PY = """#!/usr/bin/env python3
# LocalCam PC Virtual Webcam Client
# Turns your phone into a system webcam for Zoom, Teams, Meet, OBS, Discord

import sys
import os
import time
import threading
import urllib.request
import json
import tkinter as tk
from tkinter import ttk, messagebox

try:
    import cv2
    import numpy as np
    from PIL import Image, ImageTk
except ImportError as e:
    print(f"[LocalCam] Missing essential graphics library: {e}")
    print("[LocalCam] Please run: pip install -r requirements.txt")
    sys.exit(1)

try:
    import pyvirtualcam
    HAS_VIRTUAL_CAM = True
except ImportError:
    HAS_VIRTUAL_CAM = False
    print("[LocalCam] Warning: 'pyvirtualcam' not installed. Running in preview mode.")

class LocalCamClientApp:
    def __init__(self, root):
        self.root = root
        self.root.title("LocalCam PC Virtual Webcam Client")
        self.root.geometry("980x680")
        self.root.minsize(860, 580)
        self.root.configure(bg="#0A0D14")

        self.is_connected = False
        self.capture_thread = None
        self.stop_event = threading.Event()
        self.current_frame = None
        self.frame_lock = threading.Lock()

        self.fps_counter = 0
        self.fps_display = 0.0
        self.last_fps_time = time.time()
        self.stream_width = 1280
        self.stream_height = 720
        self.mirror_horizontal = tk.BooleanVar(value=True)

        self.vcam = None
        self.vcam_status_text = tk.StringVar(
            value="Virtual Camera: " + ("Ready (OBS/DirectShow/v4l2)" if HAS_VIRTUAL_CAM else "Driver Missing (Preview only)")
        )

        self._setup_styles()
        self._build_ui()
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)
        self.root.after(30, self._update_preview_canvas)
        self.root.after(1000, self._poll_phone_status)

    def _setup_styles(self):
        self.style = ttk.Style()
        self.style.theme_use("clam")
        self.style.configure("TFrame", background="#0A0D14")
        self.style.configure("Card.TFrame", background="#141923")
        self.style.configure("TLabel", background="#0A0D14", foreground="#F0F4FC", font=("Segoe UI", 10))

    def _build_ui(self):
        header = tk.Frame(self.root, bg="#141923", height=54, highlightbackground="#283248", highlightthickness=1)
        header.pack(fill=tk.X, side=tk.TOP)
        header.pack_propagate(False)

        brand_frame = tk.Frame(header, bg="#141923")
        brand_frame.pack(side=tk.LEFT, padx=16, pady=10)
        tk.Label(brand_frame, text="LOCALCAM", font=("Segoe UI", 13, "bold"), fg="#00E5FF", bg="#141923").pack(side=tk.LEFT)
        tk.Label(brand_frame, text=" | PC Virtual Webcam Bridge", font=("Segoe UI", 10), fg="#8E9BB0", bg="#141923").pack(side=tk.LEFT, padx=4)

        self.status_badge = tk.Label(header, text="● STANDBY", font=("Segoe UI", 10, "bold"), fg="#8E9BB0", bg="#1C2333", padx=12, pady=4)
        self.status_badge.pack(side=tk.RIGHT, padx=16)

        main_content = tk.Frame(self.root, bg="#0A0D14")
        main_content.pack(fill=tk.BOTH, expand=True, padx=16, pady=16)

        sidebar = tk.Frame(main_content, bg="#141923", width=330, highlightbackground="#283248", highlightthickness=1)
        sidebar.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 16))
        sidebar.pack_propagate(False)
        self._build_sidebar_controls(sidebar)

        viewport_frame = tk.Frame(main_content, bg="#05070A", highlightbackground="#283248", highlightthickness=1)
        viewport_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(viewport_frame, bg="#05070A", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True)

        footer = tk.Frame(self.root, bg="#141923", height=36, highlightbackground="#283248", highlightthickness=1)
        footer.pack(fill=tk.X, side=tk.BOTTOM)
        footer.pack_propagate(False)

        self.telemetry_lbl = tk.Label(footer, text="Resolution: -- | FPS: 0.0 | Delay: -- ms", font=("Consolas", 9), fg="#8E9BB0", bg="#141923")
        self.telemetry_lbl.pack(side=tk.LEFT, padx=16)

        vcam_lbl = tk.Label(footer, textvariable=self.vcam_status_text, font=("Segoe UI", 9), fg="#34C759" if HAS_VIRTUAL_CAM else "#FFB300", bg="#141923")
        vcam_lbl.pack(side=tk.RIGHT, padx=16)

    def _build_sidebar_controls(self, parent):
        pad_opts = {"padx": 14, "pady": (10, 4)}
        tk.Label(parent, text="CONNECTION SETTINGS", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923").pack(anchor="w", **pad_opts)

        ip_frame = tk.Frame(parent, bg="#141923")
        ip_frame.pack(fill=tk.X, padx=14, pady=3)
        tk.Label(ip_frame, text="Phone IP Address:", font=("Segoe UI", 9), fg="#F0F4FC", bg="#141923").pack(anchor="w")
        self.ip_entry = tk.Entry(ip_frame, font=("Consolas", 11), bg="#1C2333", fg="#00E5FF", insertbackground="#00E5FF", bd=1, relief=tk.SOLID)
        self.ip_entry.insert(0, "192.168.43.1")
        self.ip_entry.pack(fill=tk.X, pady=(2, 0))

        port_frame = tk.Frame(parent, bg="#141923")
        port_frame.pack(fill=tk.X, padx=14, pady=3)
        tk.Label(port_frame, text="Port:", font=("Segoe UI", 9), fg="#F0F4FC", bg="#141923").pack(side=tk.LEFT)
        self.port_entry = tk.Entry(port_frame, width=8, font=("Consolas", 10), bg="#1C2333", fg="#00E5FF", insertbackground="#00E5FF", bd=1, relief=tk.SOLID)
        self.port_entry.insert(0, "8080")
        self.port_entry.pack(side=tk.LEFT, padx=6)

        quick_btn_frame = tk.Frame(parent, bg="#141923")
        quick_btn_frame.pack(fill=tk.X, padx=14, pady=3)
        tk.Button(quick_btn_frame, text="Hotspot (192.168.43.1)", font=("Segoe UI", 8), bg="#1C2333", fg="#8E9BB0", bd=0, cursor="hand2", command=lambda: self._set_ip("192.168.43.1")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 4))
        tk.Button(quick_btn_frame, text="Localhost", font=("Segoe UI", 8), bg="#1C2333", fg="#8E9BB0", bd=0, cursor="hand2", command=lambda: self._set_ip("127.0.0.1")).pack(side=tk.RIGHT, fill=tk.X, expand=True)

        self.btn_connect = tk.Button(parent, text="▶ START WEBCAM FEED", font=("Segoe UI", 11, "bold"), bg="#00E5FF", fg="#0A0D14", bd=0, height=2, cursor="hand2", command=self.toggle_connection)
        self.btn_connect.pack(fill=tk.X, padx=14, pady=12)

        tk.Frame(parent, bg="#283248", height=1).pack(fill=tk.X, padx=14, pady=6)
        tk.Label(parent, text="WEBCAM PREFERENCES", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923").pack(anchor="w", **pad_opts)
        tk.Checkbutton(parent, text="Mirror Video Horizontally (Selfie View)", variable=self.mirror_horizontal, bg="#141923", fg="#F0F4FC", selectcolor="#1C2333", activebackground="#141923", activeforeground="#00E5FF", font=("Segoe UI", 9)).pack(anchor="w", padx=14, pady=3)

        vcam_box = tk.Frame(parent, bg="#1C2333", highlightbackground="#283248", highlightthickness=1)
        vcam_box.pack(fill=tk.X, padx=14, pady=8)
        tk.Label(vcam_box, text="Virtual Camera Target:\nUses 'Unity Video Capture' (No OBS)\nor 'OBS Virtual Camera' if installed.", font=("Segoe UI", 8), fg="#8E9BB0", bg="#1C2333", justify=tk.LEFT).pack(padx=10, pady=(8, 4), anchor="w")
        
        btn_restart_vcam = tk.Button(
            vcam_box,
            text="🔄 Reset Virtual Cam / Fix 'In Use'",
            font=("Segoe UI", 8),
            bg="#1C2333",
            fg="#00E5FF",
            bd=1,
            relief=tk.SOLID,
            cursor="hand2",
            padx=8,
            pady=3,
            command=self.restart_virtual_camera
        )
        btn_restart_vcam.pack(fill=tk.X, padx=10, pady=(0, 8))

        tk.Frame(parent, bg="#283248", height=1).pack(fill=tk.X, padx=14, pady=6)
        tk.Label(parent, text="REMOTE PHONE CONTROLS", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923").pack(anchor="w", **pad_opts)

        grid_frame = tk.Frame(parent, bg="#141923")
        grid_frame.pack(fill=tk.X, padx=14, pady=4)
        tk.Button(grid_frame, text="⚡ Torch", font=("Segoe UI", 9), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("torch")).grid(row=0, column=0, sticky="ew", padx=(0, 4), pady=3)
        tk.Button(grid_frame, text="🔄 Flip Lens", font=("Segoe UI", 9), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("switch")).grid(row=0, column=1, sticky="ew", padx=(4, 0), pady=3)
        tk.Button(grid_frame, text="🔍 Zoom +", font=("Segoe UI", 9), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("zoom_in")).grid(row=1, column=0, sticky="ew", padx=(0, 4), pady=3)
        tk.Button(grid_frame, text="1x Reset", font=("Segoe UI", 9), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("zoom_reset")).grid(row=1, column=1, sticky="ew", padx=(4, 0), pady=3)
        grid_frame.columnconfigure(0, weight=1)
        grid_frame.columnconfigure(1, weight=1)

        # Brightness Controls
        bright_frame = tk.Frame(parent, bg="#141923")
        bright_frame.pack(fill=tk.X, padx=14, pady=3)
        tk.Button(bright_frame, text="☀️ Bright -", font=("Segoe UI", 8), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("brightness_down")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 2))
        tk.Button(bright_frame, text="0 EV", font=("Segoe UI", 8), bg="#1C2333", fg="#00E5FF", bd=0, cursor="hand2", command=lambda: self.send_remote_control("brightness_reset")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=2)
        tk.Button(bright_frame, text="☀️ Bright +", font=("Segoe UI", 8), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("brightness_up")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(2, 0))

        # Focus & Auto-Optimize
        opt_frame = tk.Frame(parent, bg="#141923")
        opt_frame.pack(fill=tk.X, padx=14, pady=3)
        tk.Button(opt_frame, text="🎯 Auto-Focus (AF)", font=("Segoe UI", 9), bg="#1C2333", fg="#F0F4FC", bd=0, cursor="hand2", command=lambda: self.send_remote_control("focus")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 2))
        tk.Button(opt_frame, text="⚡ Auto-Optimize", font=("Segoe UI", 9, "bold"), bg="#1C2333", fg="#00E5FF", bd=0, cursor="hand2", command=lambda: self.send_remote_control("auto_optimize")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(2, 0))

        tk.Button(parent, text="📸 Save Hi-Res Photo to Phone", font=("Segoe UI", 9), bg="#1C2333", fg="#00E5FF", bd=0, cursor="hand2", command=lambda: self.send_remote_control("snap")).pack(fill=tk.X, padx=14, pady=(6, 12))

    def _set_ip(self, ip_str):
        self.ip_entry.delete(0, tk.END)
        self.ip_entry.insert(0, ip_str)

    def toggle_connection(self):
        if not self.is_connected:
            self.start_streaming()
        else:
            self.stop_streaming()

    def start_streaming(self):
        ip = self.ip_entry.get().strip()
        port = self.port_entry.get().strip()
        if not ip or not port:
            messagebox.showerror("Invalid Input", "Please specify a valid Phone IP and Port.")
            return

        stream_url = f"http://{ip}:{port}/stream.mjpg"
        self.stop_event.clear()
        self.is_connected = True
        self.btn_connect.config(text="⏹ STOP WEBCAM FEED", bg="#FF3B30", fg="#FFFFFF")
        self.status_badge.config(text="● CONNECTING...", fg="#FFB300", bg="#2E2412")

        self.capture_thread = threading.Thread(target=self._stream_capture_worker, args=(stream_url,), daemon=True)
        self.capture_thread.start()

    def stop_streaming(self):
        self.stop_event.set()
        self.is_connected = False
        if self.vcam:
            try:
                self.vcam.close()
            except Exception:
                pass
            self.vcam = None

        self.btn_connect.config(text="▶ START WEBCAM FEED", bg="#00E5FF", fg="#0A0D14")
        self.status_badge.config(text="● STANDBY", fg="#8E9BB0", bg="#1C2333")
        self.telemetry_lbl.config(text="Resolution: -- | FPS: 0.0 | Delay: -- ms")
        self.canvas.delete("all")

    def _stream_capture_worker(self, stream_url):
        cap = cv2.VideoCapture(stream_url)
        if not cap.isOpened():
            self._fallback_mjpeg_stream_worker(stream_url)
            return

        self.root.after(0, lambda: self.status_badge.config(text="● LIVE TO VIRTUAL CAM", fg="#FF3B30", bg="#361517"))
        err_count = 0
        while not self.stop_event.is_set():
            ret, frame = cap.read()
            if not ret or frame is None:
                err_count += 1
                if err_count > 40:
                    break
                time.sleep(0.02)
                continue
            err_count = 0
            self._process_and_dispatch_frame(frame)
        cap.release()
        if not self.stop_event.is_set():
            self.root.after(0, self.stop_streaming)

    def _fallback_mjpeg_stream_worker(self, stream_url):
        try:
            req = urllib.request.Request(stream_url, headers={"User-Agent": "LocalCam-PC-Client/1.0"})
            stream = urllib.request.urlopen(req, timeout=8)
            bytes_data = b""
            self.root.after(0, lambda: self.status_badge.config(text="● LIVE TO VIRTUAL CAM", fg="#FF3B30", bg="#361517"))
            while not self.stop_event.is_set():
                chunk = stream.read(4096)
                if not chunk:
                    break
                bytes_data += chunk
                a = bytes_data.find(b"\xff\xd8")
                b = bytes_data.find(b"\xff\xd9")
                if a != -1 and b != -1 and b > a:
                    jpg = bytes_data[a : b + 2]
                    bytes_data = bytes_data[b + 2 :]
                    frame = cv2.imdecode(np.frombuffer(jpg, dtype=np.uint8), cv2.IMREAD_COLOR)
                    if frame is not None:
                        self._process_and_dispatch_frame(frame)
        except Exception:
            pass
        finally:
            if not self.stop_event.is_set():
                self.root.after(0, self.stop_streaming)

    def restart_virtual_camera(self):
        if self.vcam is not None:
            try:
                self.vcam.close()
            except Exception:
                pass
            self.vcam = None
        self.root.after(0, lambda: self.vcam_status_text.set("Virtual Camera: Resetting (Will re-link on next frame)"))
        messagebox.showinfo(
            "Virtual Camera Reset",
            "Virtual Camera device handle was reset.\n\n"
            "If VideoPsalm or Google Meet showed 'Webcam not available / in use':\n"
            "1. Make sure other apps like Zoom, Teams, or browser tabs with Google Meet are closed.\n"
            "2. Ensure 'START WEBCAM FEED' is active here FIRST.\n"
            "3. In VideoPsalm, select 'Unity Video Capture' in Video settings."
        )

    def _process_and_dispatch_frame(self, bgr_frame):
        if self.mirror_horizontal.get():
            bgr_frame = cv2.flip(bgr_frame, 1)

        h, w = bgr_frame.shape[:2]
        self.stream_width = w
        self.stream_height = h
        rgb_frame = cv2.cvtColor(bgr_frame, cv2.COLOR_BGR2RGB)

        if HAS_VIRTUAL_CAM:
            try:
                if self.vcam is None or self.vcam.width != w or self.vcam.height != h:
                    if self.vcam is not None:
                        try:
                            self.vcam.close()
                        except Exception:
                            pass

                    backends = ["unitycapture", "obs", None] if sys.platform.startswith("win") else [None, "v4l2loopback"]
                    last_err = None
                    self.vcam = None
                    for b in backends:
                        try:
                            kwargs = {"width": w, "height": h, "fps": 30, "fmt": pyvirtualcam.PixelFormat.RGB}
                            if b:
                                kwargs["backend"] = b
                            self.vcam = pyvirtualcam.Camera(**kwargs)
                            actual_dev = getattr(self.vcam, "device", "Unity Video Capture" if b == "unitycapture" else "Virtual Camera")
                            self.vcam.send(rgb_frame)
                            self.root.after(0, lambda d=actual_dev: (
                                self.vcam_status_text.set(f"Virtual Camera: Active ({d})")
                            ))
                            break
                        except Exception as be:
                            last_err = be
                            continue

                    if self.vcam is None:
                        raise last_err or RuntimeError("No virtual camera backend available")

                self.vcam.send(rgb_frame)
                self.vcam.sleep_until_next_frame()
            except Exception as e:
                self.root.after(0, lambda: self.vcam_status_text.set(f"Virtual Cam Error: {e}"))

        self.fps_counter += 1
        now = time.time()
        if now - self.last_fps_time >= 1.0:
            self.fps_display = self.fps_counter / (now - self.last_fps_time)
            self.fps_counter = 0
            self.last_fps_time = now

        with self.frame_lock:
            self.current_frame = rgb_frame

    def _update_preview_canvas(self):
        if self.is_connected:
            frame_to_draw = None
            with self.frame_lock:
                if self.current_frame is not None:
                    frame_to_draw = self.current_frame.copy()

            if frame_to_draw is not None:
                canvas_w = self.canvas.winfo_width()
                canvas_h = self.canvas.winfo_height()
                if canvas_w > 10 and canvas_h > 10:
                    img_h, img_w = frame_to_draw.shape[:2]
                    scale = min(canvas_w / img_w, canvas_h / img_h)
                    new_w = max(1, int(img_w * scale))
                    new_h = max(1, int(img_h * scale))
                    resized = cv2.resize(frame_to_draw, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
                    self.tk_image = ImageTk.PhotoImage(image=Image.fromarray(resized))
                    self.canvas.delete("all")
                    self.canvas.create_image((canvas_w - new_w) // 2, (canvas_h - new_h) // 2, anchor=tk.NW, image=self.tk_image)
                    self.telemetry_lbl.config(text=f"Resolution: {self.stream_width}x{self.stream_height} | FPS: {self.fps_display:.1f} | VirtualCam: {'ON' if self.vcam else 'STANDBY'}")

        self.root.after(30, self._update_preview_canvas)

    def _poll_phone_status(self):
        if self.is_connected:
            ip = self.ip_entry.get().strip()
            port = self.port_entry.get().strip()
            threading.Thread(target=self._fetch_status_worker, args=(f"http://{ip}:{port}/api/status",), daemon=True).start()
        self.root.after(2500, self._poll_phone_status)

    def _fetch_status_worker(self, url):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "LocalCam-PC-Client"})
            with urllib.request.urlopen(req, timeout=1.5) as response:
                data = json.loads(response.read().decode("utf-8"))
                clients = data.get("clients", 1)
                fps = data.get("fps", 0)
                res = data.get("resolution", "")
                self.root.after(0, lambda: self.telemetry_lbl.config(text=f"Resolution: {res or f'{self.stream_width}x{self.stream_height}'} | PC FPS: {self.fps_display:.1f} | Phone FPS: {fps:.1f} | Clients: {clients}"))
        except Exception:
            pass

    def send_remote_control(self, action):
        ip = self.ip_entry.get().strip()
        port = self.port_entry.get().strip()
        if not ip or not port:
            return
        url = f"http://{ip}:{port}/api/control?action={action}"
        def _send():
            try:
                urllib.request.urlopen(url, timeout=2.0)
            except Exception:
                pass
        threading.Thread(target=_send, daemon=True).start()

    def on_close(self):
        self.stop_streaming()
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = LocalCamClientApp(root)
    root.mainloop()
"""

    fun getPcInstructionsHtml(hostAddress: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LocalCam PC Virtual Webcam &mdash; Setup Guide</title>
    <style>
        :root {
            --bg-color: #0A0D14;
            --surface-color: #141923;
            --surface-border: #283248;
            --accent-cyan: #00E5FF;
            --accent-green: #34C759;
            --text-primary: #F0F4FC;
            --text-secondary: #8E9BB0;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: var(--bg-color);
            color: var(--text-primary);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
            padding: 24px;
            max-width: 860px;
            margin: 0 auto;
            line-height: 1.6;
        }
        header {
            margin-bottom: 30px;
            border-bottom: 1px solid var(--surface-border);
            padding-bottom: 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        h1 { font-size: 22px; color: var(--accent-cyan); display: flex; align-items: center; gap: 10px; }
        .badge { background: rgba(0, 229, 255, 0.15); color: var(--accent-cyan); padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
        .card {
            background: var(--surface-color);
            border: 1px solid var(--surface-border);
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 20px;
        }
        .card-title { font-size: 16px; font-weight: 700; color: var(--accent-cyan); margin-bottom: 12px; }
        .btn {
            background: var(--accent-cyan);
            color: #0A0D14;
            border: none;
            padding: 10px 18px;
            border-radius: 8px;
            font-weight: 700;
            cursor: pointer;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        .btn-outline {
            background: #1C2333;
            color: var(--text-primary);
            border: 1px solid var(--surface-border);
        }
        pre, code {
            background: #06080D;
            border: 1px solid var(--surface-border);
            border-radius: 6px;
            padding: 4px 8px;
            font-family: Consolas, monospace;
            color: var(--accent-cyan);
            font-size: 13px;
        }
        pre { padding: 12px; overflow-x: auto; margin: 10px 0; }
        ol, ul { padding-left: 20px; margin: 10px 0; }
        li { margin-bottom: 8px; color: var(--text-secondary); }
        li b { color: var(--text-primary); }
    </style>
</head>
<body>
    <header>
        <div>
            <h1><span>📹</span> LocalCam PC Virtual Webcam</h1>
            <div style="font-size: 13px; color: var(--text-secondary); margin-top: 4px;">
                Use your phone as a hardware webcam in Zoom, Microsoft Teams, Google Meet, Skype, and OBS.
            </div>
        </div>
        <a href="/" class="btn btn-outline">&larr; Back to Live Stream</a>
    </header>

    <!-- OBS-Free Virtual Camera Registration Card -->
    <div class="card" style="border: 1px solid #FFB300; background: #18150F;">
        <div class="card-title" style="color: #FFB300;">⚡ Using VideoPsalm, Google Meet, or Zoom WITHOUT OBS?</div>
        <div style="color: var(--text-secondary); font-size: 13px; margin-bottom: 14px;">
            OBS Studio is <b>NOT required</b>! Windows simply needs a lightweight DirectShow virtual camera driver registered once:
        </div>
        <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px;">
            <a href="/client/install_virtual_camera.bat" download="install_virtual_camera.bat" class="btn" style="background: #FFB300; color: #000; font-weight: bold;">
                <span>🔧 Download Driver Installer (install_virtual_camera.bat)</span>
            </a>
            <a href="/client/driver/UnityCaptureFilter64.dll" download="UnityCaptureFilter64.dll" class="btn btn-outline">
                <span>📁 UnityCaptureFilter64.dll (157 KB)</span>
            </a>
            <a href="/client/driver/UnityCaptureFilter32.dll" download="UnityCaptureFilter32.dll" class="btn btn-outline">
                <span>📁 UnityCaptureFilter32.dll (168 KB)</span>
            </a>
        </div>
        <div style="font-size: 13px; color: #D1D5DB; line-height: 1.5;">
            <b>How to register:</b><br>
            1. Right-click <code>install_virtual_camera.bat</code> &rarr; <b>Run as administrator</b>.<br>
            2. It registers in 2 seconds.<br>
            3. Open <b>VideoPsalm</b> or <b>Google Meet</b> &rarr; Settings &rarr; Video &rarr; Select <b>"Unity Video Capture"</b>!
        </div>
    </div>

    <!-- Download Card -->
    <div class="card" style="border: 1px solid var(--accent-cyan); background: linear-gradient(180deg, #141A28 0%, #101520 100%);">
        <div class="card-title">1-Click Downloads for your Laptop</div>
        <div style="color: var(--text-secondary); font-size: 13px; margin-bottom: 16px;">
            Download the lightweight client files directly from the phone to your PC:
        </div>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
            <a href="/client/start_pc_client.bat" download="start_pc_client.bat" class="btn">
                <span>⊞ Download Windows Launcher (.bat)</span>
            </a>
            <a href="/client/install_virtual_camera.bat" download="install_virtual_camera.bat" class="btn btn-outline" style="border-color: #FFB300; color: #FFB300;">
                <span>🔧 install_virtual_camera.bat (No OBS)</span>
            </a>
            <a href="/client/localcam_pc_client.py" download="localcam_pc_client.py" class="btn btn-outline">
                <span>🐍 Download Python App (.py)</span>
            </a>
            <a href="/client/requirements.txt" download="requirements.txt" class="btn btn-outline">
                <span>📄 requirements.txt</span>
            </a>
            <a href="/client/start_pc_client.sh" download="start_pc_client.sh" class="btn btn-outline">
                <span>🐧 Download Mac/Linux (.sh)</span>
            </a>
        </div>
    </div>

    <!-- Quick Run Guide -->
    <div class="card">
        <div class="card-title">How It Works</div>
        <ol>
            <li>Ensure your laptop is connected to this phone's <b>Hotspot</b> or Wi-Fi network.</li>
            <li>Make sure <b>Python 3.8+</b> is installed on your PC (from <a href="https://www.python.org" target="_blank" style="color: var(--accent-cyan)">python.org</a>).</li>
            <li><b>If you don't have OBS</b>: Run <code>install_virtual_camera.bat</code> as Administrator once.</li>
            <li>Double-click <code>start_pc_client.bat</code> (Windows) or run <code>./start_pc_client.sh</code> (Mac/Linux).</li>
            <li>The app will automatically connect to <code>$hostAddress</code> and start the Virtual Camera.</li>
            <li>In <b>VideoPsalm, Google Meet, Zoom, or Teams</b>: open Camera settings and choose <b>"Unity Video Capture"</b> (or <b>"OBS Virtual Camera"</b>)!</li>
        </ol>
    </div>

    <!-- Quick Command Line -->
    <div class="card">
        <div class="card-title">Terminal / Command Line Quickstart</div>
        <div style="font-size: 13px; color: var(--text-secondary);">
            Or install and run in 3 commands from terminal:
        </div>
        <pre>pip install pyvirtualcam opencv-python Pillow requests numpy
curl -O $hostAddress/client/localcam_pc_client.py
python localcam_pc_client.py</pre>
    </div>
</body>
</html>
"""
    }
}
