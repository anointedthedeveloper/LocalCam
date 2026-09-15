package com.example.data

data class StreamStats(
    val currentFps: Float = 0f,
    val currentBitrateKbps: Long = 0L,
    val totalFramesSent: Long = 0L,
    val totalBytesSent: Long = 0L,
    val activeResolution: String = "1280x720",
    val uptimeSeconds: Long = 0L
) {
    val formattedBitrate: String
        get() = if (currentBitrateKbps >= 1000) {
            String.format(java.util.Locale.US, "%.1f Mbps", currentBitrateKbps / 1000.0)
        } else {
            "$currentBitrateKbps kbps"
        }

    val formattedFps: String
        get() = String.format(java.util.Locale.US, "%.1f fps", currentFps)
}
