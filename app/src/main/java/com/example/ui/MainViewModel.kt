package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraManager
import com.example.data.CameraLens
import com.example.data.CameraSettings
import com.example.data.ConnectedClient
import com.example.data.ConnectionState
import com.example.data.NetworkType
import com.example.data.StreamStats
import com.example.data.StreamingStatus
import com.example.network.NetworkHelper
import com.example.server.LocalStreamServer
import com.example.service.LocalCamStreamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val networkHelper = NetworkHelper(application)
    private var unregisterNetworkObserver: (() -> Unit)? = null

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _cameraSettings = MutableStateFlow(CameraSettings())
    val cameraSettings: StateFlow<CameraSettings> = _cameraSettings.asStateFlow()

    private val _streamStats = MutableStateFlow(StreamStats())
    val streamStats: StateFlow<StreamStats> = _streamStats.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var streamServer: LocalStreamServer? = null
    private var statsJob: Job? = null

    var cameraManager: CameraManager? = null

    init {
        refreshNetworkState()
        unregisterNetworkObserver = networkHelper.observeNetworkChanges {
            refreshNetworkState()
        }
    }

    fun initCameraManager(manager: CameraManager) {
        cameraManager = manager

        manager.onZoomStateChanged = { current, min, max ->
            _cameraSettings.update {
                it.copy(
                    zoomRatio = current,
                    minZoomRatio = min,
                    maxZoomRatio = max
                )
            }
        }

        manager.onTorchAvailabilityChanged = { _ ->
            // Torch available
        }

        manager.onExposureStateChanged = { current, min, max ->
            _cameraSettings.update {
                it.copy(
                    exposureCompensation = current,
                    minExposureIndex = min,
                    maxExposureIndex = max
                )
            }
        }

        manager.onPhotoCaptureSuccess = { uri ->
            _userMessage.value = "Photo saved to Gallery! (Pictures/LocalCam)"
        }

        manager.onPhotoCaptureError = { err ->
            _userMessage.value = "Failed to save photo: $err"
        }
    }

    fun refreshNetworkState() {
        viewModelScope.launch(Dispatchers.IO) {
            val netInfo = networkHelper.getActiveNetworkInfo()
            _connectionState.update { current ->
                current.copy(
                    networkType = netInfo.type,
                    localIpAddress = netInfo.ipAddress
                )
            }
        }
    }

    fun startStreaming() {
        if (_connectionState.value.streamingStatus == StreamingStatus.STREAMING) return

        val port = _connectionState.value.port
        val server = LocalStreamServer(
            port = port,
            onClientCountChanged = { clients ->
                _connectionState.update { it.copy(connectedClients = clients) }
            },
            onRemoteControlReceived = { action, params ->
                handleRemoteControl(action, params)
            },
            getStatusJson = {
                buildStatusJson()
            }
        )

        streamServer = server
        server.start()

        _connectionState.update {
            it.copy(streamingStatus = StreamingStatus.STREAMING)
        }

        val streamUrl = _connectionState.value.streamUrl ?: "http://localhost:$port"
        LocalCamStreamingService.startService(getApplication(), streamUrl)

        startStatsPolling()
    }

    fun stopStreaming() {
        streamServer?.stop()
        streamServer = null
        statsJob?.cancel()
        statsJob = null

        _connectionState.update {
            it.copy(
                streamingStatus = StreamingStatus.STOPPED,
                connectedClients = emptyList()
            )
        }
        _streamStats.value = StreamStats()

        LocalCamStreamingService.stopService(getApplication())
    }

    fun toggleStreaming() {
        if (_connectionState.value.streamingStatus == StreamingStatus.STREAMING) {
            stopStreaming()
        } else {
            startStreaming()
        }
    }

    fun onFrameAvailable(jpegBytes: ByteArray, width: Int, height: Int) {
        streamServer?.onFrameAvailable(jpegBytes, width, height)
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _connectionState.value.streamingStatus == StreamingStatus.STREAMING) {
                delay(1000)
                streamServer?.let { server ->
                    _streamStats.value = server.currentStats
                }
            }
        }
    }

    private fun handleRemoteControl(action: String, params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.Main) {
            when (action) {
                "torch" -> toggleTorch()
                "switch" -> switchCameraLens()
                "zoom_in" -> {
                    val current = _cameraSettings.value.zoomRatio
                    val next = (current + 0.5f).coerceAtMost(_cameraSettings.value.maxZoomRatio)
                    setZoomRatio(next)
                    _userMessage.value = "Zoom set to ${String.format(java.util.Locale.US, "%.1fx", next)}"
                }
                "zoom_reset" -> {
                    setZoomRatio(1.0f)
                    _userMessage.value = "Zoom reset to 1x"
                }
                "snap" -> snapPhoto()
            }
        }
    }

    private fun buildStatusJson(): String {
        val settings = _cameraSettings.value
        val stats = _streamStats.value
        val clients = _connectionState.value.connectedClients.size
        return """
            {
                "streaming": true,
                "resolution": "${settings.targetResolution.label}",
                "fps": ${stats.currentFps},
                "bitrateKbps": ${stats.currentBitrateKbps},
                "zoom": ${settings.zoomRatio},
                "torch": ${settings.isTorchEnabled},
                "camera": "${if (settings.lens == CameraLens.BACK) "back" else "front"}",
                "clients": $clients,
                "device": "${Build.MANUFACTURER} ${Build.MODEL}"
            }
        """.trimIndent()
    }

    fun setZoomRatio(ratio: Float) {
        val bounded = ratio.coerceIn(
            _cameraSettings.value.minZoomRatio,
            _cameraSettings.value.maxZoomRatio
        )
        _cameraSettings.update { it.copy(zoomRatio = bounded) }
        cameraManager?.setZoomRatio(bounded)
    }

    fun toggleTorch() {
        val newState = !_cameraSettings.value.isTorchEnabled
        _cameraSettings.update { it.copy(isTorchEnabled = newState) }
        cameraManager?.toggleTorch(newState)
    }

    fun switchCameraLens() {
        val newLens = if (_cameraSettings.value.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        _cameraSettings.update {
            it.copy(
                lens = newLens,
                zoomRatio = 1.0f,
                isTorchEnabled = false
            )
        }
    }

    fun updateCameraSettings(newSettings: CameraSettings) {
        _cameraSettings.value = newSettings
    }

    fun snapPhoto() {
        cameraManager?.snapPhoto()
    }

    fun openHotspotSettings(): android.content.Intent {
        return networkHelper.openHotspotSettings()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
        unregisterNetworkObserver?.invoke()
        cameraManager?.release()
    }
}
