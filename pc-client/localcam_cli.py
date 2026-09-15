#!/usr/bin/env python3
"""
LocalCam PC Virtual Webcam - CLI / Headless Runner
Stream straight from Android Phone into Windows/Mac/Linux Virtual Camera.
Usage:
    python localcam_cli.py --ip 192.168.43.1 --port 8080
"""

import sys
import time
import argparse
import urllib.request

try:
    import cv2
    import numpy as np
except ImportError:
    print("[Error] Missing OpenCV: run 'pip install opencv-python numpy'")
    sys.exit(1)

try:
    import pyvirtualcam
except ImportError:
    print("[Error] Missing pyvirtualcam: run 'pip install pyvirtualcam'")
    sys.exit(1)


def run_cli_stream(ip, port, mirror=True, requested_fps=30):
    stream_url = f"http://{ip}:{port}/stream.mjpg"
    print(f"==================================================")
    print(f"  LocalCam CLI Virtual Webcam")
    print(f"  Target: {stream_url}")
    print(f"  Mirror: {mirror}")
    print(f"==================================================")
    print("Connecting to phone video feed...")

    cap = cv2.VideoCapture(stream_url)
    if not cap.isOpened():
        print(f"[Error] Could not open video stream at {stream_url}")
        print("Please verify the phone is running LocalCam and streaming is started.")
        sys.exit(1)

    # Read first frame to determine dimensions
    ret, frame = cap.read()
    if not ret or frame is None:
        print("[Error] Failed to read initial frame from camera.")
        sys.exit(1)

    h, w = frame.shape[:2]
    print(f"[LocalCam] Stream detected: {w}x{h} resolution")
    print(f"[LocalCam] Launching Virtual Camera device...")

    backends = ["unityvideo", "obs", None] if sys.platform.startswith("win") else [None, "v4l2loopback"]
    cam = None
    last_err = None
    for b in backends:
        try:
            kwargs = {"width": w, "height": h, "fps": requested_fps, "fmt": pyvirtualcam.PixelFormat.RGB}
            if b:
                kwargs["backend"] = b
            cam = pyvirtualcam.Camera(**kwargs)
            break
        except Exception as e:
            last_err = e
            continue

    if cam is None:
        print("\n[ERROR] Could not start Virtual Camera driver!")
        print(f"Details: {last_err}")
        print("\nOn Windows without OBS Studio:")
        print("Run 'install_virtual_camera.bat' as Administrator to register 'Unity Video Capture'.")
        print("No OBS Studio installation is needed!")
        sys.exit(1)

    try:
        with cam:
            print(f"[LocalCam] ✓ Virtual Camera online: '{cam.device}'")
            print(f"[LocalCam] Select '{cam.device}' in VideoPsalm, Google Meet, Zoom, Teams, OBS, etc.")
            print("[LocalCam] Press Ctrl+C to stop streaming.")

            fps_counter = 0
            last_time = time.time()

            while True:
                ret, frame = cap.read()
                if not ret or frame is None:
                    time.sleep(0.01)
                    continue

                if mirror:
                    frame = cv2.flip(frame, 1)

                rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                cam.send(rgb_frame)
                cam.sleep_until_next_frame()

                fps_counter += 1
                now = time.time()
                if now - last_time >= 2.0:
                    fps = fps_counter / (now - last_time)
                    print(f"\r[Streaming] {w}x{h} @ {fps:.1f} FPS -> {cam.device}", end="", flush=True)
                    fps_counter = 0
                    last_time = now

    except KeyboardInterrupt:
        print("\n[LocalCam] Streaming stopped by user.")
    except Exception as e:
        print(f"\n[Error] Virtual camera error: {e}")
    finally:
        cap.release()


def main():
    parser = argparse.ArgumentParser(description="LocalCam PC Virtual Webcam Runner")
    parser.add_argument("--ip", default="192.168.43.1", help="Android Phone IP (default: 192.168.43.1)")
    parser.add_argument("--port", type=int, default=8080, help="LocalCam HTTP port (default: 8080)")
    parser.add_argument("--no-mirror", action="store_true", help="Disable horizontal selfie mirroring")
    parser.add_argument("--fps", type=int, default=30, help="Target FPS (default: 30)")

    args = parser.parse_args()
    run_cli_stream(
        ip=args.ip,
        port=args.port,
        mirror=not args.no_mirror,
        requested_fps=args.fps
    )


if __name__ == "__main__":
    main()
