package com.example.server

object WebReceiverHtml {

    fun getReceiverHtml(streamUrl: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LocalCam &mdash; Wireless Presentation Camera</title>
    <style>
        :root {
            --bg-color: #0A0D14;
            --surface-color: #161B26;
            --surface-border: #232B3E;
            --accent-cyan: #00E5FF;
            --accent-red: #FF3B30;
            --accent-green: #34C759;
            --text-primary: #F0F4FC;
            --text-secondary: #8E9BB0;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-primary);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        header {
            background-color: rgba(22, 27, 38, 0.95);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--surface-border);
            padding: 12px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .brand-logo {
            width: 28px;
            height: 28px;
            border-radius: 6px;
            background: linear-gradient(135deg, #00E5FF, #0077FF);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 900;
            font-size: 14px;
            color: #0A0D14;
        }

        .brand-title {
            font-size: 18px;
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        .brand-badge {
            font-size: 10px;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 12px;
            background-color: rgba(255, 59, 48, 0.18);
            color: var(--accent-red);
            border: 1px solid rgba(255, 59, 48, 0.4);
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }

        .brand-badge::before {
            content: '';
            width: 6px;
            height: 6px;
            background-color: var(--accent-red);
            border-radius: 50%;
            animation: pulse 1.5s infinite;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(0.85); }
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .btn {
            background-color: var(--surface-color);
            color: var(--text-primary);
            border: 1px solid var(--surface-border);
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.15s ease;
            user-select: none;
        }

        .btn:hover {
            background-color: #1F2737;
            border-color: #313C52;
        }

        .btn:active {
            transform: scale(0.97);
        }

        .btn-primary {
            background-color: var(--accent-cyan);
            color: #0A0D14;
            border-color: var(--accent-cyan);
        }

        .btn-primary:hover {
            background-color: #33EBFF;
        }

        main {
            flex: 1;
            display: flex;
            flex-direction: column;
            padding: 20px;
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
            gap: 20px;
        }

        .viewport-wrapper {
            position: relative;
            background-color: #000000;
            border-radius: 12px;
            border: 1px solid var(--surface-border);
            overflow: hidden;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 480px;
        }

        .viewport-wrapper.fullscreen {
            border-radius: 0;
            border: none;
        }

        #video-feed {
            width: 100%;
            height: 100%;
            max-height: 78vh;
            object-fit: contain;
            display: block;
        }

        .osd-overlay {
            position: absolute;
            top: 14px;
            left: 14px;
            right: 14px;
            display: flex;
            justify-content: space-between;
            pointer-events: none;
        }

        .osd-pill {
            background: rgba(10, 13, 20, 0.75);
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255, 255, 255, 0.12);
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            color: var(--text-primary);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .dot-green {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--accent-green);
            box-shadow: 0 0 8px var(--accent-green);
        }

        .controls-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
            gap: 16px;
        }

        .card {
            background-color: var(--surface-color);
            border: 1px solid var(--surface-border);
            border-radius: 12px;
            padding: 16px 20px;
        }

        .card-title {
            font-size: 14px;
            font-weight: 700;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.8px;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .card-row {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        .info-metric {
            display: flex;
            flex-direction: column;
            gap: 4px;
            flex: 1;
            min-width: 90px;
        }

        .info-label {
            font-size: 11px;
            color: var(--text-secondary);
            font-weight: 500;
        }

        .info-value {
            font-size: 15px;
            font-weight: 700;
            color: var(--text-primary);
            font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
        }

        .obs-link-box {
            background: #0D111A;
            border: 1px solid #232B3E;
            border-radius: 8px;
            padding: 8px 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-family: monospace;
            font-size: 13px;
            color: var(--accent-cyan);
            margin-top: 8px;
        }

        .toast {
            position: fixed;
            bottom: 24px;
            right: 24px;
            background: rgba(16, 22, 34, 0.95);
            border: 1px solid var(--accent-cyan);
            color: var(--text-primary);
            padding: 12px 20px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            box-shadow: 0 8px 24px rgba(0,0,0,0.5);
            opacity: 0;
            transform: translateY(12px);
            transition: all 0.25s ease;
            pointer-events: none;
            z-index: 999;
        }

        .toast.show {
            opacity: 1;
            transform: translateY(0);
        }
    </style>
</head>
<body>
    <header>
        <div class="brand">
            <div class="brand-logo">LC</div>
            <div class="brand-title">LocalCam</div>
            <div class="brand-badge">LIVE ON HOTSPOT</div>
        </div>
        <div class="header-actions">
            <button class="btn" onclick="takeSnapshot()">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
                Download Snapshot
            </button>
            <button class="btn btn-primary" onclick="toggleFullscreen()">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
                Full Screen
            </button>
        </div>
    </header>

    <main>
        <div class="viewport-wrapper" id="viewport">
            <img id="video-feed" src="/video" alt="LocalCam Live Video Stream" onerror="handleStreamError()">
            <div class="osd-overlay">
                <div class="osd-pill">
                    <span class="dot-green"></span>
                    <span id="osd-status">CONNECTING...</span>
                </div>
                <div class="osd-pill" id="osd-time">00:00:00</div>
            </div>
        </div>

        <div class="controls-grid">
            <!-- Camera Remote Controls -->
            <div class="card">
                <div class="card-title">Remote Camera Controls</div>
                <div class="card-row">
                    <button class="btn" onclick="sendControl('torch')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                        Torch
                    </button>
                    <button class="btn" onclick="sendControl('switch')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 16l4-4-4-4"/><path d="M4 20h10a4 4 0 0 0 4-4V8"/><path d="M4 8l-4 4 4 4"/><path d="M20 4H10a4 4 0 0 0-4 4v8"/></svg>
                        Flip Camera
                    </button>
                    <button class="btn" onclick="sendControl('zoom_in')">Zoom +</button>
                    <button class="btn" onclick="sendControl('zoom_reset')">1x Reset</button>
                    <button class="btn" onclick="sendControl('snap')">Trigger Snap</button>
                </div>
            </div>

            <!-- PC Virtual Webcam Client (Zoom, Teams, Meet) -->
            <div class="card" style="border-color: rgba(0, 229, 255, 0.4); background: linear-gradient(180deg, #161D2B 0%, #121722 100%);">
                <div class="card-title" style="color: var(--accent-cyan); display: flex; justify-content: space-between; align-items: center;">
                    <span>PC Virtual Webcam Client</span>
                    <span style="font-size: 10px; background: rgba(0, 229, 255, 0.15); color: var(--accent-cyan); padding: 3px 8px; border-radius: 10px; font-weight: 700;">SYSTEM WEBCAM</span>
                </div>
                <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px;">
                    Output this wireless video feed directly into <b>Zoom, Microsoft Teams, Google Meet, Skype, Discord, & OBS</b> as a native PC webcam:
                </div>
                <div class="card-row" style="gap: 8px;">
                    <a href="/client/start_pc_client.bat" download="start_pc_client.bat" class="btn" style="text-decoration: none; display: inline-flex; align-items: center; gap: 6px; background: var(--accent-cyan); color: #0A0D14; font-weight: 700;">
                        <span>⬇ Windows Launcher (.bat)</span>
                    </a>
                    <a href="/pc" class="btn" style="text-decoration: none; display: inline-flex; align-items: center; gap: 6px; background: #1F2738;">
                        <span>Setup Guide & Mac/Linux</span>
                    </a>
                </div>
            </div>

            <!-- OBS Studio & Presentation Source Guide -->
            <div class="card">
                <div class="card-title">OBS Studio & Presentation Source</div>
                <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.5;">
                    Add as <b>Browser Source</b> in OBS or presentation software (vMix / ProPresenter):
                </div>
                <div class="obs-link-box">
                    <span id="obs-url">$streamUrl/obs</span>
                    <button class="btn" style="padding: 4px 8px; font-size: 11px;" onclick="copyObsUrl()">Copy URL</button>
                </div>
            </div>

            <!-- Real-Time Diagnostics -->
            <div class="card">
                <div class="card-title">Real-Time Diagnostics</div>
                <div class="card-row">
                    <div class="info-metric">
                        <span class="info-label">RESOLUTION</span>
                        <span class="info-value" id="diag-res">--</span>
                    </div>
                    <div class="info-metric">
                        <span class="info-label">PHONE FPS</span>
                        <span class="info-value" id="diag-fps">--</span>
                    </div>
                    <div class="info-metric">
                        <span class="info-label">BITRATE</span>
                        <span class="info-value" id="diag-bitrate">--</span>
                    </div>
                    <div class="info-metric">
                        <span class="info-label">CLIENTS</span>
                        <span class="info-value" id="diag-clients">1</span>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <div id="toast" class="toast">Action Sent</div>

    <script>
        const video = document.getElementById('video-feed');
        const osdStatus = document.getElementById('osd-status');
        const osdTime = document.getElementById('osd-time');
        const viewport = document.getElementById('viewport');

        let startTime = Date.now();
        setInterval(() => {
            const elapsed = Math.floor((Date.now() - startTime) / 1000);
            const hrs = String(Math.floor(elapsed / 3600)).padStart(2, '0');
            const mins = String(Math.floor((elapsed % 3600) / 60)).padStart(2, '0');
            const secs = String(elapsed % 60).padStart(2, '0');
            osdTime.textContent = hrs + ':' + mins + ':' + secs;
        }, 1000);

        function handleStreamError() {
            osdStatus.textContent = "STREAM RECONNECTING...";
            setTimeout(() => {
                video.src = "/video?t=" + Date.now();
            }, 1500);
        }

        video.onload = function() {
            osdStatus.textContent = "ONLINE &bull; LOW LATENCY";
        };

        function toggleFullscreen() {
            if (!document.fullscreenElement) {
                viewport.requestFullscreen().catch(err => alert("Fullscreen error: " + err.message));
                viewport.classList.add('fullscreen');
            } else {
                document.exitFullscreen();
                viewport.classList.remove('fullscreen');
            }
        }

        document.addEventListener('fullscreenchange', () => {
            if (!document.fullscreenElement) {
                viewport.classList.remove('fullscreen');
            }
        });

        function takeSnapshot() {
            window.open('/snapshot.jpg?download=1', '_blank');
        }

        function copyObsUrl() {
            const url = document.getElementById('obs-url').textContent;
            navigator.clipboard.writeText(url).then(() => {
                showToast("OBS URL copied to clipboard!");
            }).catch(() => {
                showToast("Selected URL: " + url);
            });
        }

        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.textContent = msg;
            toast.classList.add('show');
            setTimeout(() => toast.classList.remove('show'), 2200);
        }

        function sendControl(action) {
            fetch('/api/control?action=' + encodeURIComponent(action), { method: 'POST' })
                .then(res => res.json())
                .then(data => {
                    showToast(data.message || "Command executed");
                })
                .catch(err => {
                    showToast("Control sent");
                });
        }

        // Periodic telemetry poll
        setInterval(() => {
            fetch('/api/status')
                .then(r => r.json())
                .then(data => {
                    if (data.resolution) document.getElementById('diag-res').textContent = data.resolution;
                    if (data.fps !== undefined) document.getElementById('diag-fps').textContent = data.fps.toFixed(1) + ' fps';
                    if (data.bitrateKbps !== undefined) {
                        const kbps = data.bitrateKbps;
                        document.getElementById('diag-bitrate').textContent = kbps >= 1000 ? (kbps / 1000).toFixed(1) + ' Mbps' : kbps + ' kbps';
                    }
                    if (data.clients !== undefined) document.getElementById('diag-clients').textContent = data.clients;
                })
                .catch(() => {});
        }, 2000);
    </script>
</body>
</html>
        """.trimIndent()
    }

    fun getObsHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>LocalCam OBS Source</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body {
            width: 100%;
            height: 100%;
            background: transparent;
            overflow: hidden;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        img {
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: block;
        }
    </style>
</head>
<body>
    <img src="/video" alt="LocalCam Stream" onerror="setTimeout(() => { location.reload(); }, 1500);">
</body>
</html>
        """.trimIndent()
    }
}
