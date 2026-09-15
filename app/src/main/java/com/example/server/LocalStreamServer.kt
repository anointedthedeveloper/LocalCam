package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.ConnectedClient
import com.example.data.StreamStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LocalStreamServer(
    private val port: Int = 8080,
    private val context: Context? = null,
    private val onClientCountChanged: (List<ConnectedClient>) -> Unit,
    private val onRemoteControlReceived: (String, Map<String, String>) -> Unit,
    private val getStatusJson: () -> String
) {
    companion object {
        private const val TAG = "LocalStreamServer"
        private const val BOUNDARY = "--frame"
    }

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Map of active video streaming clients (Socket -> OutputStream)
    private val videoClients = ConcurrentHashMap<Socket, OutputStream>()
    private val clientInfoMap = ConcurrentHashMap<Socket, ConnectedClient>()

    // Latest single frame for /snapshot.jpg
    @Volatile
    private var latestFrame: ByteArray? = null

    // Telemetry counters
    private val framesSentCount = AtomicLong(0)
    private val bytesSentCount = AtomicLong(0)
    private var lastFpsCalculationTime = System.currentTimeMillis()
    private var framesSinceLastCalculation = 0
    private var bytesSinceLastCalculation = 0L

    @Volatile
    var currentStats = StreamStats()
        private set

    fun start() {
        if (isRunning.getAndSet(true)) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "LocalCam server started on port $port")

                while (isActive && isRunning.get()) {
                    val socket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: SocketException) {
                        break
                    }

                    launch {
                        handleClientConnection(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error", e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        Log.i(TAG, "Stopping LocalCam server...")

        videoClients.forEach { (socket, _) ->
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
        videoClients.clear()
        clientInfoMap.clear()
        notifyClientsChanged()

        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
        serverJob?.cancel()
    }

    fun onFrameAvailable(jpegBytes: ByteArray, width: Int, height: Int) {
        if (!isRunning.get()) return

        latestFrame = jpegBytes

        // Calculate real-time FPS and bitrate
        val now = System.currentTimeMillis()
        framesSinceLastCalculation++
        bytesSinceLastCalculation += jpegBytes.size

        val timeDiff = now - lastFpsCalculationTime
        if (timeDiff >= 1000) {
            val fps = (framesSinceLastCalculation * 1000f) / timeDiff
            val bitrateKbps = (bytesSinceLastCalculation * 8L) / timeDiff
            currentStats = currentStats.copy(
                currentFps = fps,
                currentBitrateKbps = bitrateKbps,
                totalFramesSent = framesSentCount.get(),
                totalBytesSent = bytesSentCount.get(),
                activeResolution = "${width}x${height}"
            )
            framesSinceLastCalculation = 0
            bytesSinceLastCalculation = 0L
            lastFpsCalculationTime = now
        }

        if (videoClients.isEmpty()) return

        val headerString = "\r\n$BOUNDARY\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: ${jpegBytes.size}\r\n\r\n"
        val headerBytes = headerString.toByteArray(StandardCharsets.US_ASCII)

        val deadSockets = mutableListOf<Socket>()

        videoClients.forEach { (socket, outStream) ->
            try {
                outStream.write(headerBytes)
                outStream.write(jpegBytes)
                outStream.flush()

                framesSentCount.incrementAndGet()
                bytesSentCount.addAndGet((headerBytes.size + jpegBytes.size).toLong())
            } catch (e: Exception) {
                deadSockets.add(socket)
            }
        }

        if (deadSockets.isNotEmpty()) {
            deadSockets.forEach { socket ->
                videoClients.remove(socket)
                clientInfoMap.remove(socket)
                try {
                    socket.close()
                } catch (ignored: Exception) {}
            }
            notifyClientsChanged()
        }
    }

    private suspend fun handleClientConnection(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 0 // Keep streaming without timeout

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: return@withContext

            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return@withContext
            }

            val method = parts[0].uppercase()
            val uriWithQuery = parts[1]
            val path = uriWithQuery.substringBefore("?")
            val queryString = if (uriWithQuery.contains("?")) uriWithQuery.substringAfter("?") else ""

            // Read remaining headers
            var userAgent = "Unknown"
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                if (line!!.startsWith("User-Agent:", ignoreCase = true)) {
                    userAgent = line!!.substringAfter(":").trim()
                }
            }

            val outputStream = BufferedOutputStream(socket.getOutputStream())

            when {
                path == "/" || path == "/index.html" -> {
                    val clientAddress = socket.inetAddress?.hostAddress ?: "127.0.0.1"
                    val hostHeader = "$clientAddress:$port"
                    val html = WebReceiverHtml.getReceiverHtml("http://$hostHeader")
                    val bytes = html.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/obs" -> {
                    val html = WebReceiverHtml.getObsHtml()
                    val bytes = html.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/video" || path == "/stream.mjpg" -> {
                    val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
                    val client = ConnectedClient(
                        ipAddress = clientIp,
                        userAgent = userAgent
                    )

                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Connection: close\r\n" +
                            "Server: LocalCam/1.0\r\n" +
                            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Content-Type: multipart/x-mixed-replace; boundary=$BOUNDARY\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.flush()

                    videoClients[socket] = outputStream
                    clientInfoMap[socket] = client
                    notifyClientsChanged()
                    // Keep socket open for continuous streaming in onFrameAvailable
                }

                path == "/snapshot.jpg" -> {
                    val frame = latestFrame
                    if (frame != null) {
                        val isDownload = queryString.contains("download=1")
                        val disposition = if (isDownload) "Content-Disposition: attachment; filename=\"localcam_snapshot.jpg\"\r\n" else ""
                        val response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: image/jpeg\r\n" +
                                "Content-Length: ${frame.size}\r\n" +
                                disposition +
                                "Cache-Control: no-cache\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Connection: close\r\n\r\n"
                        outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                        outputStream.write(frame)
                        outputStream.flush()
                    } else {
                        val errorMsg = "No frame available yet"
                        val response = "HTTP/1.1 503 Service Unavailable\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: ${errorMsg.length}\r\n" +
                                "Connection: close\r\n\r\n$errorMsg"
                        outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                        outputStream.flush()
                    }
                    socket.close()
                }

                path == "/api/status" -> {
                    val json = getStatusJson()
                    val bytes = json.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/api/control" -> {
                    val queryParams = parseQueryParams(queryString)
                    val action = queryParams["action"] ?: "unknown"
                    onRemoteControlReceived(action, queryParams)

                    val json = "{\"status\":\"ok\",\"action\":\"$action\",\"message\":\"Action received\"}"
                    val bytes = json.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/pc" || path == "/client" -> {
                    val clientAddress = socket.inetAddress?.hostAddress ?: "127.0.0.1"
                    val hostHeader = "http://$clientAddress:$port"
                    val html = PcClientDistribution.getPcInstructionsHtml(hostHeader)
                    val bytes = html.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/localcam_pc_client.py" -> {
                    val bytes = try {
                        context?.assets?.open("client/localcam_pc_client.py")?.use { it.readBytes() }
                    } catch (e: Exception) { null } ?: PcClientDistribution.LOCALCAM_PC_CLIENT_PY.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/x-python; charset=utf-8\r\n" +
                            "Content-Disposition: attachment; filename=\"localcam_pc_client.py\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/start_pc_client.bat" || path == "/download/client" -> {
                    val bytes = try {
                        context?.assets?.open("client/start_pc_client.bat")?.use { it.readBytes() }
                    } catch (e: Exception) { null } ?: PcClientDistribution.START_PC_CLIENT_BAT.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/x-bat\r\n" +
                            "Content-Disposition: attachment; filename=\"start_pc_client.bat\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/install_virtual_camera.bat" || path == "/client/install.bat" -> {
                    val bytes = try {
                        context?.assets?.open("client/install_virtual_camera.bat")?.use { it.readBytes() }
                    } catch (e: Exception) { null } ?: PcClientDistribution.INSTALL_VIRTUAL_CAMERA_BAT.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/x-bat\r\n" +
                            "Content-Disposition: attachment; filename=\"install_virtual_camera.bat\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/uninstall_virtual_camera.bat" -> {
                    val bytes = try {
                        context?.assets?.open("client/uninstall_virtual_camera.bat")?.use { it.readBytes() }
                    } catch (e: Exception) { null } ?: PcClientDistribution.UNINSTALL_VIRTUAL_CAMERA_BAT.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/x-bat\r\n" +
                            "Content-Disposition: attachment; filename=\"uninstall_virtual_camera.bat\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/driver/UnityCaptureFilter64.dll" || path == "/client/UnityCaptureFilter64.dll" -> {
                    val bytes = try {
                        context?.assets?.open("driver/UnityCaptureFilter64.dll")?.use { it.readBytes() }
                    } catch (e: Exception) { null }
                    if (bytes != null) {
                        val response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Disposition: attachment; filename=\"UnityCaptureFilter64.dll\"\r\n" +
                                "Content-Length: ${bytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                        outputStream.write(bytes)
                    } else {
                        val notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n"
                        outputStream.write(notFound.toByteArray(StandardCharsets.US_ASCII))
                    }
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/driver/UnityCaptureFilter32.dll" || path == "/client/UnityCaptureFilter32.dll" -> {
                    val bytes = try {
                        context?.assets?.open("driver/UnityCaptureFilter32.dll")?.use { it.readBytes() }
                    } catch (e: Exception) { null }
                    if (bytes != null) {
                        val response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Disposition: attachment; filename=\"UnityCaptureFilter32.dll\"\r\n" +
                                "Content-Length: ${bytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                        outputStream.write(bytes)
                    } else {
                        val notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n"
                        outputStream.write(notFound.toByteArray(StandardCharsets.US_ASCII))
                    }
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/start_pc_client.sh" -> {
                    val bytes = PcClientDistribution.START_PC_CLIENT_SH.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/x-sh\r\n" +
                            "Content-Disposition: attachment; filename=\"start_pc_client.sh\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                path == "/client/requirements.txt" -> {
                    val bytes = PcClientDistribution.REQUIREMENTS_TXT.toByteArray(StandardCharsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Disposition: attachment; filename=\"requirements.txt\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.write(bytes)
                    outputStream.flush()
                    socket.close()
                }

                else -> {
                    val error = "Not Found"
                    val response = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: ${error.length}\r\n" +
                            "Connection: close\r\n\r\n$error"
                    outputStream.write(response.toByteArray(StandardCharsets.US_ASCII))
                    outputStream.flush()
                    socket.close()
                }
            }
        } catch (e: Exception) {
            // Client closed or interrupted
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                result[key] = value
            }
        }
        return result
    }

    private fun notifyClientsChanged() {
        val clients = clientInfoMap.values.toList()
        onClientCountChanged(clients)
    }
}
