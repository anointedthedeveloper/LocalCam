#!/usr/bin/env python3
"""
LocalCam PC Client
Turns your Android phone into a high-performance, low-latency PC Virtual Webcam.
Compatible with Zoom, Google Meet, Microsoft Teams, Discord, Skype, OBS Studio, and Web Browsers.
"""

import sys
import os
import time
import threading
import urllib.request
import json
import tkinter as tk
from tkinter import ttk, messagebox

# Optional dependencies check
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
    print("[LocalCam] Warning: 'pyvirtualcam' not installed. Running in preview-only mode.")
    print("[LocalCam] To enable virtual webcam for Zoom/Meet/Teams, run: pip install pyvirtualcam")

try:
    import requests
except ImportError:
    requests = None


class LocalCamClientApp:
    def __init__(self, root):
        self.root = root
        self.root.title("LocalCam PC Virtual Webcam Client")
        self.root.geometry("980x680")
        self.root.minsize(860, 580)
        self.root.configure(bg="#0A0D14")

        # Streaming state variables
        self.is_connected = False
        self.capture_thread = None
        self.stop_event = threading.Event()
        self.current_frame = None
        self.frame_lock = threading.Lock()

        # Telemetry
        self.fps_counter = 0
        self.fps_display = 0.0
        self.last_fps_time = time.time()
        self.stream_width = 1280
        self.stream_height = 720
        self.mirror_horizontal = tk.BooleanVar(value=True)

        # Virtual Cam instance
        self.vcam = None
        self.driver_warned = False
        self.detected_camera_device = tk.StringVar(value="Detecting camera driver...")
        self.vcam_status_text = tk.StringVar(
            value="Virtual Camera: " + ("Ready (OBS/DirectShow/v4l2)" if HAS_VIRTUAL_CAM else "Driver Missing (Preview only)")
        )

        # Build UI
        self._setup_styles()
        self._build_ui()

        # Check driver immediately
        self.root.after(200, self._detect_virtual_cam_driver)

        # Window close handler
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

        # Periodically refresh preview frame & status
        self.root.after(30, self._update_preview_canvas)
        self.root.after(1000, self._poll_phone_status)

    def _setup_styles(self):
        self.style = ttk.Style()
        self.style.theme_use("clam")
        
        # Configure modern dark theme colors
        self.style.configure("TFrame", background="#0A0D14")
        self.style.configure("Card.TFrame", background="#141923")
        self.style.configure("TLabel", background="#0A0D14", foreground="#F0F4FC", font=("Segoe UI", 10))
        self.style.configure("Card.TLabel", background="#141923", foreground="#F0F4FC", font=("Segoe UI", 10))
        self.style.configure("Sub.TLabel", background="#141923", foreground="#8E9BB0", font=("Segoe UI", 9))
        self.style.configure("Header.TLabel", background="#141923", foreground="#00E5FF", font=("Segoe UI", 11, "bold"))
        self.style.configure("Brand.TLabel", background="#0A0D14", foreground="#00E5FF", font=("Segoe UI", 14, "bold"))

    def _build_ui(self):
        # Top Header Bar
        header = tk.Frame(self.root, bg="#141923", height=54, bd=0, highlightbackground="#283248", highlightthickness=1)
        header.pack(fill=tk.X, side=tk.TOP)
        header.pack_propagate(False)

        brand_frame = tk.Frame(header, bg="#141923")
        brand_frame.pack(side=tk.LEFT, padx=16, pady=10)

        brand_lbl = tk.Label(brand_frame, text="LOCALCAM", font=("Segoe UI", 13, "bold"), fg="#00E5FF", bg="#141923")
        brand_lbl.pack(side=tk.LEFT)

        sub_brand = tk.Label(brand_frame, text=" | PC Virtual Webcam Bridge", font=("Segoe UI", 10), fg="#8E9BB0", bg="#141923")
        sub_brand.pack(side=tk.LEFT, padx=4)

        # Status badge in header
        self.status_badge = tk.Label(
            header,
            text="● STANDBY",
            font=("Segoe UI", 10, "bold"),
            fg="#8E9BB0",
            bg="#1C2333",
            padx=12,
            pady=4
        )
        self.status_badge.pack(side=tk.RIGHT, padx=16)

        # Main Layout: Left controls sidebar, Right video monitor
        main_content = tk.Frame(self.root, bg="#0A0D14")
        main_content.pack(fill=tk.BOTH, expand=True, padx=16, pady=16)

        # Left Sidebar (Controls)
        sidebar = tk.Frame(main_content, bg="#141923", width=330, bd=0, highlightbackground="#283248", highlightthickness=1)
        sidebar.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 16))
        sidebar.pack_propagate(False)

        self._build_sidebar_controls(sidebar)

        # Right Video Viewport
        viewport_frame = tk.Frame(main_content, bg="#05070A", bd=0, highlightbackground="#283248", highlightthickness=1)
        viewport_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(viewport_frame, bg="#05070A", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True)

        # Bottom Telemetry Bar
        footer = tk.Frame(self.root, bg="#141923", height=36, highlightbackground="#283248", highlightthickness=1)
        footer.pack(fill=tk.X, side=tk.BOTTOM)
        footer.pack_propagate(False)

        self.telemetry_lbl = tk.Label(
            footer,
            text="Resolution: -- | FPS: 0.0 | Delay: -- ms | Clients: 0",
            font=("Consolas", 9),
            fg="#8E9BB0",
            bg="#141923"
        )
        self.telemetry_lbl.pack(side=tk.LEFT, padx=16)

        vcam_lbl = tk.Label(
            footer,
            textvariable=self.vcam_status_text,
            font=("Segoe UI", 9),
            fg="#34C759" if HAS_VIRTUAL_CAM else "#FFB300",
            bg="#141923"
        )
        vcam_lbl.pack(side=tk.RIGHT, padx=16)

    def _build_sidebar_controls(self, parent):
        pad_opts = {"padx": 14, "pady": (10, 4)}

        # Section 1: Connection Hub
        conn_title = tk.Label(parent, text="CONNECTION SETTINGS", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923")
        conn_title.pack(anchor="w", **pad_opts)

        # IP Address input
        ip_frame = tk.Frame(parent, bg="#141923")
        ip_frame.pack(fill=tk.X, padx=14, pady=3)

        tk.Label(ip_frame, text="Phone IP Address:", font=("Segoe UI", 9), fg="#F0F4FC", bg="#141923").pack(anchor="w")
        self.ip_entry = tk.Entry(ip_frame, font=("Consolas", 11), bg="#1C2333", fg="#00E5FF", insertbackground="#00E5FF", bd=1, relief=tk.SOLID)
        self.ip_entry.insert(0, "192.168.43.1")  # Default Android Hotspot IP
        self.ip_entry.pack(fill=tk.X, pady=(2, 0))

        # Port input & Quick IP buttons
        port_frame = tk.Frame(parent, bg="#141923")
        port_frame.pack(fill=tk.X, padx=14, pady=3)

        tk.Label(port_frame, text="Port:", font=("Segoe UI", 9), fg="#F0F4FC", bg="#141923").pack(side=tk.LEFT)
        self.port_entry = tk.Entry(port_frame, width=8, font=("Consolas", 10), bg="#1C2333", fg="#00E5FF", insertbackground="#00E5FF", bd=1, relief=tk.SOLID)
        self.port_entry.insert(0, "8080")
        self.port_entry.pack(side=tk.LEFT, padx=6)

        # Quick preset buttons (Hotspot / Localhost)
        quick_btn_frame = tk.Frame(parent, bg="#141923")
        quick_btn_frame.pack(fill=tk.X, padx=14, pady=3)

        btn_hotspot = tk.Button(
            quick_btn_frame,
            text="Hotspot (192.168.43.1)",
            font=("Segoe UI", 8),
            bg="#1C2333",
            fg="#8E9BB0",
            bd=0,
            cursor="hand2",
            command=lambda: self._set_ip("192.168.43.1")
        )
        btn_hotspot.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 4))

        btn_local = tk.Button(
            quick_btn_frame,
            text="Localhost",
            font=("Segoe UI", 8),
            bg="#1C2333",
            fg="#8E9BB0",
            bd=0,
            cursor="hand2",
            command=lambda: self._set_ip("127.0.0.1")
        )
        btn_local.pack(side=tk.RIGHT, fill=tk.X, expand=True)

        # Big Connect / Disconnect Button
        self.btn_connect = tk.Button(
            parent,
            text="▶ START WEBCAM FEED",
            font=("Segoe UI", 11, "bold"),
            bg="#00E5FF",
            fg="#0A0D14",
            activebackground="#00B8D4",
            activeforeground="#0A0D14",
            bd=0,
            height=2,
            cursor="hand2",
            command=self.toggle_connection
        )
        self.btn_connect.pack(fill=tk.X, padx=14, pady=12)

        # Divider
        tk.Frame(parent, bg="#283248", height=1).pack(fill=tk.X, padx=14, pady=6)

        # Section 2: Webcam Output Options
        opts_title = tk.Label(parent, text="WEBCAM PREFERENCES", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923")
        opts_title.pack(anchor="w", **pad_opts)

        cb_mirror = tk.Checkbutton(
            parent,
            text="Mirror Video Horizontally (Selfie View)",
            variable=self.mirror_horizontal,
            bg="#141923",
            fg="#F0F4FC",
            selectcolor="#1C2333",
            activebackground="#141923",
            activeforeground="#00E5FF",
            font=("Segoe UI", 9)
        )
        cb_mirror.pack(anchor="w", padx=14, pady=3)

        # Virtual Cam info box
        vcam_box = tk.Frame(parent, bg="#1C2333", bd=0, highlightbackground="#283248", highlightthickness=1)
        vcam_box.pack(fill=tk.X, padx=14, pady=8)
        
        tk.Label(
            vcam_box,
            text="WEBCAM SOURCE IN APPS:",
            font=("Segoe UI", 8, "bold"),
            fg="#00E5FF",
            bg="#1C2333"
        ).pack(padx=10, pady=(8, 2), anchor="w")

        self.lbl_vcam_dev = tk.Label(
            vcam_box,
            textvariable=self.detected_camera_device,
            font=("Segoe UI", 9, "bold"),
            fg="#FFFFFF",
            bg="#1C2333"
        )
        self.lbl_vcam_dev.pack(padx=10, pady=(0, 4), anchor="w")

        tk.Label(
            vcam_box,
            text="In VideoPsalm, Google Meet, Zoom, or Teams,\nselect this camera name in Settings > Video.",
            font=("Segoe UI", 8),
            fg="#8E9BB0",
            bg="#1C2333",
            justify=tk.LEFT
        ).pack(padx=10, pady=(0, 6), anchor="w")

        btn_install_drv = tk.Button(
            vcam_box,
            text="🔧 Install Virtual Cam Driver (No OBS)",
            font=("Segoe UI", 8, "bold"),
            bg="#00E5FF",
            fg="#0A0D14",
            bd=0,
            cursor="hand2",
            padx=8,
            pady=4,
            command=self.install_virtual_camera_driver
        )
        btn_install_drv.pack(fill=tk.X, padx=10, pady=(0, 8))

        # Divider
        tk.Frame(parent, bg="#283248", height=1).pack(fill=tk.X, padx=14, pady=6)

        # Section 3: Remote Phone Hardware Controls
        ctrl_title = tk.Label(parent, text="REMOTE PHONE CONTROLS", font=("Segoe UI", 9, "bold"), fg="#8E9BB0", bg="#141923")
        ctrl_title.pack(anchor="w", **pad_opts)

        grid_frame = tk.Frame(parent, bg="#141923")
        grid_frame.pack(fill=tk.X, padx=14, pady=4)

        # Row 1 of remote controls: Flashlight & Switch Lens
        btn_torch = tk.Button(
            grid_frame,
            text="⚡ Torch Toggle",
            font=("Segoe UI", 9),
            bg="#1C2333",
            fg="#F0F4FC",
            bd=0,
            height=1,
            cursor="hand2",
            command=lambda: self.send_remote_control("torch")
        )
        btn_torch.grid(row=0, column=0, sticky="ew", padx=(0, 4), pady=3)

        btn_lens = tk.Button(
            grid_frame,
            text="🔄 Flip Lens",
            font=("Segoe UI", 9),
            bg="#1C2333",
            fg="#F0F4FC",
            bd=0,
            height=1,
            cursor="hand2",
            command=lambda: self.send_remote_control("switch")
        )
        btn_lens.grid(row=0, column=1, sticky="ew", padx=(4, 0), pady=3)

        # Row 2: Zoom in & Zoom reset
        btn_zoom_in = tk.Button(
            grid_frame,
            text="🔍 Zoom +",
            font=("Segoe UI", 9),
            bg="#1C2333",
            fg="#F0F4FC",
            bd=0,
            height=1,
            cursor="hand2",
            command=lambda: self.send_remote_control("zoom_in")
        )
        btn_zoom_in.grid(row=1, column=0, sticky="ew", padx=(0, 4), pady=3)

        btn_zoom_rst = tk.Button(
            grid_frame,
            text="1x Zoom Reset",
            font=("Segoe UI", 9),
            bg="#1C2333",
            fg="#F0F4FC",
            bd=0,
            height=1,
            cursor="hand2",
            command=lambda: self.send_remote_control("zoom_reset")
        )
        btn_zoom_rst.grid(row=1, column=1, sticky="ew", padx=(4, 0), pady=3)

        grid_frame.columnconfigure(0, weight=1)
        grid_frame.columnconfigure(1, weight=1)

        # Remote High-Res Snapshot trigger
        btn_snap = tk.Button(
            parent,
            text="📸 Save Hi-Res Photo to Phone",
            font=("Segoe UI", 9),
            bg="#1C2333",
            fg="#00E5FF",
            bd=0,
            cursor="hand2",
            command=lambda: self.send_remote_control("snap")
        )
        btn_snap.pack(fill=tk.X, padx=14, pady=(6, 12))

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

        # Update UI state
        self.is_connected = True
        self.btn_connect.config(
            text="⏹ STOP WEBCAM FEED",
            bg="#FF3B30",
            fg="#FFFFFF"
        )
        self.status_badge.config(
            text="● CONNECTING...",
            fg="#FFB300",
            bg="#2E2412"
        )

        # Start background capture thread
        self.capture_thread = threading.Thread(
            target=self._stream_capture_worker,
            args=(stream_url,),
            daemon=True
        )
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

        self.btn_connect.config(
            text="▶ START WEBCAM FEED",
            bg="#00E5FF",
            fg="#0A0D14"
        )
        self.status_badge.config(
            text="● STANDBY",
            fg="#8E9BB0",
            bg="#1C2333"
        )
        self.telemetry_lbl.config(
            text="Resolution: -- | FPS: 0.0 | Delay: -- ms | Clients: 0"
        )

        # Clear canvas
        self.canvas.delete("all")
        self.canvas.create_text(
            self.canvas.winfo_width() // 2 or 300,
            self.canvas.winfo_height() // 2 or 200,
            text="LocalCam Standby\nClick 'START WEBCAM FEED' to stream video into Virtual Camera",
            font=("Segoe UI", 12),
            fill="#5A667A",
            justify=tk.CENTER
        )

    def _stream_capture_worker(self, stream_url):
        """Worker thread that consumes the MJPEG stream and outputs to the Virtual Camera."""
        print(f"[LocalCam] Connecting to stream: {stream_url}")

        # Try OpenCV VideoCapture first for high-performance decoding
        cap = cv2.VideoCapture(stream_url)
        
        # Test if opened successfully
        if not cap.isOpened():
            print("[LocalCam] OpenCV capture failed, falling back to HTTP stream parser...")
            self._fallback_mjpeg_stream_worker(stream_url)
            return

        # Connected
        self.root.after(0, lambda: self.status_badge.config(
            text="● LIVE TO VIRTUAL CAM",
            fg="#FF3B30",
            bg="#361517"
        ))

        consecutive_errors = 0

        while not self.stop_event.is_set():
            ret, frame = cap.read()
            if not ret or frame is None:
                consecutive_errors += 1
                if consecutive_errors > 40:
                    print("[LocalCam] Stream disconnected.")
                    break
                time.sleep(0.02)
                continue

            consecutive_errors = 0
            self._process_and_dispatch_frame(frame)

        cap.release()
        if not self.stop_event.is_set():
            self.root.after(0, self.stop_streaming)

    def _fallback_mjpeg_stream_worker(self, stream_url):
        """Direct multipart/x-mixed-replace HTTP chunk decoder if OpenCV VideoCapture fails."""
        try:
            req = urllib.request.Request(
                stream_url,
                headers={"User-Agent": "LocalCam-PC-Client/1.0"}
            )
            stream = urllib.request.urlopen(req, timeout=8)
            bytes_data = b""

            self.root.after(0, lambda: self.status_badge.config(
                text="● LIVE TO VIRTUAL CAM",
                fg="#FF3B30",
                bg="#361517"
            ))

            while not self.stop_event.is_set():
                chunk = stream.read(4096)
                if not chunk:
                    break
                bytes_data += chunk
                a = bytes_data.find(b"\xff\xd8")  # JPEG start
                b = bytes_data.find(b"\xff\xd9")  # JPEG end
                if a != -1 and b != -1 and b > a:
                    jpg = bytes_data[a : b + 2]
                    bytes_data = bytes_data[b + 2 :]
                    frame = cv2.imdecode(np.frombuffer(jpg, dtype=np.uint8), cv2.IMREAD_COLOR)
                    if frame is not None:
                        self._process_and_dispatch_frame(frame)
        except Exception as e:
            print(f"[LocalCam] Fallback stream error: {e}")
            self.root.after(0, lambda: messagebox.showerror(
                "Connection Failed",
                f"Could not connect to phone at {stream_url}.\n\n"
                "Please verify:\n"
                "1. Phone hotspot or Wi-Fi is connected\n"
                "2. LocalCam is running and 'STREAM' is active\n"
                "3. IP address and port match"
            ))
        finally:
            if not self.stop_event.is_set():
                self.root.after(0, self.stop_streaming)

    def _process_and_dispatch_frame(self, bgr_frame):
        # Apply horizontal mirror if checked (standard for webcams)
        if self.mirror_horizontal.get():
            bgr_frame = cv2.flip(bgr_frame, 1)

        h, w = bgr_frame.shape[:2]
        self.stream_width = w
        self.stream_height = h

        # Send to Virtual Camera (RGB format)
        rgb_frame = cv2.cvtColor(bgr_frame, cv2.COLOR_BGR2RGB)

        if HAS_VIRTUAL_CAM:
            try:
                if self.vcam is None or self.vcam.width != w or self.vcam.height != h:
                    if self.vcam is not None:
                        try:
                            self.vcam.close()
                        except Exception:
                            pass
                    print(f"[LocalCam] Initializing virtual camera device at {w}x{h}...")

                    # Try unityvideo FIRST (for standalone Windows users without OBS), then obs, then default
                    backends = ["unityvideo", "obs", None] if sys.platform.startswith("win") else [None, "v4l2loopback"]
                    last_err = None
                    self.vcam = None
                    for b in backends:
                        try:
                            kwargs = {"width": w, "height": h, "fps": 30, "fmt": pyvirtualcam.PixelFormat.RGB}
                            if b:
                                kwargs["backend"] = b
                            self.vcam = pyvirtualcam.Camera(**kwargs)
                            actual_dev = getattr(self.vcam, "device", "Virtual Camera")
                            print(f"[LocalCam] Active virtual camera: '{actual_dev}' using backend '{b or 'default'}'")
                            self.root.after(0, lambda d=actual_dev: (
                                self.vcam_status_text.set(f"Virtual Camera: Active ({d})"),
                                self.detected_camera_device.set(d)
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
                # Driver might not be initialized yet
                self.root.after(0, lambda: self.vcam_status_text.set(f"Virtual Cam Error: {e}"))
                if not self.driver_warned:
                    self.driver_warned = True
                    self.root.after(500, self._prompt_missing_driver_dialog)

        # Calculate FPS
        self.fps_counter += 1
        now = time.time()
        if now - self.last_fps_time >= 1.0:
            self.fps_display = self.fps_counter / (now - self.last_fps_time)
            self.fps_counter = 0
            self.last_fps_time = now

        # Store latest frame for GUI preview canvas
        with self.frame_lock:
            self.current_frame = rgb_frame

    def _update_preview_canvas(self):
        """Draws the live frame scaled to the Tkinter canvas."""
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
                    pil_img = Image.fromarray(resized)
                    self.tk_image = ImageTk.PhotoImage(image=pil_img)

                    self.canvas.delete("all")
                    # Center frame in canvas
                    x_offset = (canvas_w - new_w) // 2
                    y_offset = (canvas_h - new_h) // 2
                    self.canvas.create_image(x_offset, y_offset, anchor=tk.NW, image=self.tk_image)

                    # Update telemetry text
                    self.telemetry_lbl.config(
                        text=f"Resolution: {self.stream_width}x{self.stream_height} | FPS: {self.fps_display:.1f} | VirtualCam: {'ON' if self.vcam else 'STANDBY'}"
                    )

        self.root.after(30, self._update_preview_canvas)

    def _poll_phone_status(self):
        """Periodically requests status JSON from the phone to keep diagnostics up to date."""
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
                self.root.after(0, lambda: self.telemetry_lbl.config(
                    text=f"Resolution: {res or f'{self.stream_width}x{self.stream_height}'} | PC FPS: {self.fps_display:.1f} | Phone FPS: {fps:.1f} | Clients: {clients}"
                ))
        except Exception:
            pass

    def send_remote_control(self, action):
        """Sends remote command to the phone (torch, zoom, switch camera lens, snap photo)."""
        ip = self.ip_entry.get().strip()
        port = self.port_entry.get().strip()
        if not ip or not port:
            return

        url = f"http://{ip}:{port}/api/control?action={action}"

        def _send():
            try:
                urllib.request.urlopen(url, timeout=2.0)
            except Exception as e:
                print(f"[LocalCam] Control action failed: {e}")

        threading.Thread(target=_send, daemon=True).start()

    def _detect_virtual_cam_driver(self):
        """Probes the OS to find available DirectShow or virtual camera drivers."""
        if not HAS_VIRTUAL_CAM:
            self.vcam_status_text.set("Virtual Camera: 'pyvirtualcam' package missing")
            self.detected_camera_device.set("Preview Only (pip install pyvirtualcam)")
            return

        backends = ["unityvideo", "obs", None] if sys.platform.startswith("win") else [None, "v4l2loopback"]
        for b in backends:
            try:
                kwargs = {"width": 640, "height": 480, "fps": 30, "fmt": pyvirtualcam.PixelFormat.RGB}
                if b:
                    kwargs["backend"] = b
                test_cam = pyvirtualcam.Camera(**kwargs)
                dev_name = getattr(test_cam, "device", "Virtual Camera")
                test_cam.close()
                self.vcam_status_text.set(f"Virtual Camera: Ready ({dev_name})")
                self.detected_camera_device.set(f"'{dev_name}'")
                return
            except Exception:
                continue

        self.vcam_status_text.set("Virtual Cam: No Driver Found (Run install_virtual_camera.bat)")
        self.detected_camera_device.set("Driver Missing (Click 'Install Driver' below)")

    def install_virtual_camera_driver(self):
        """Runs the lightweight DirectShow installer without needing OBS Studio."""
        script_name = "install_virtual_camera.bat"
        base_dir = os.path.dirname(os.path.abspath(__file__))
        candidates = [
            os.path.join(base_dir, script_name),
            os.path.join(base_dir, "driver", script_name),
            os.path.join(os.getcwd(), script_name),
            os.path.join(os.getcwd(), "pc-client", script_name)
        ]
        found = None
        for p in candidates:
            if os.path.isfile(p):
                found = p
                break

        if found and sys.platform.startswith("win"):
            try:
                os.startfile(found)
                messagebox.showinfo(
                    "Driver Setup Launched",
                    "Launched the Virtual Camera installer!\n\n"
                    "1. Click 'Yes' on the Windows Administrator prompt.\n"
                    "2. The installer will register 'Unity Video Capture' in 2 seconds.\n"
                    "3. In VideoPsalm, Google Meet, Zoom, or Teams, select 'Unity Video Capture'!"
                )
                self.root.after(3500, self._detect_virtual_cam_driver)
            except Exception as e:
                messagebox.showerror("Execution Error", f"Could not launch installer: {e}")
        else:
            messagebox.showinfo(
                "Driver Setup",
                "To register the Virtual Camera driver without OBS:\n\n"
                "1. Locate 'install_virtual_camera.bat' in the pc-client folder.\n"
                "2. Right-click and select 'Run as administrator'.\n"
                "3. Reopen VideoPsalm or Google Meet and select 'Unity Video Capture'!"
            )

    def _prompt_missing_driver_dialog(self):
        """Alerts the user when video is streaming but Windows lacks a virtual camera driver."""
        resp = messagebox.askyesno(
            "Virtual Camera Driver Required",
            "Video stream is connected, but Windows has no Virtual Camera driver registered!\n\n"
            "Because of this, Google Meet and VideoPsalm cannot see the camera yet.\n\n"
            "OBS Studio is NOT required! Would you like to run the lightweight (150 KB) driver installer now?",
            icon="warning"
        )
        if resp:
            self.install_virtual_camera_driver()

    def on_close(self):
        self.stop_streaming()
        self.root.destroy()


def main():
    root = tk.Tk()
    app = LocalCamClientApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
