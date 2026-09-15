package com.example.data

enum class NetworkType {
    HOTSPOT,
    WIFI,
    DISCONNECTED
}

enum class StreamingStatus {
    STOPPED,
    STARTING,
    STREAMING,
    ERROR
}

data class ConnectedClient(
    val ipAddress: String,
    val userAgent: String,
    val connectedAtMillis: Long = System.currentTimeMillis()
)

data class ConnectionState(
    val networkType: NetworkType = NetworkType.DISCONNECTED,
    val localIpAddress: String? = null,
    val port: Int = 8080,
    val streamingStatus: StreamingStatus = StreamingStatus.STOPPED,
    val connectedClients: List<ConnectedClient> = emptyList(),
    val errorMessage: String? = null
) {
    val streamUrl: String?
        get() = localIpAddress?.let { "http://$it:$port" }

    val videoStreamUrl: String?
        get() = localIpAddress?.let { "http://$it:$port/video" }

    val hasConnectedClients: Boolean
        get() = connectedClients.isNotEmpty()
}
