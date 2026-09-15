package com.example.data

import android.util.Size

enum class CameraLens {
    BACK,
    FRONT
}

enum class StreamResolution(val label: String, val width: Int, val height: Int) {
    RES_1080P("1080p FHD", 1920, 1080),
    RES_720P("720p HD", 1280, 720),
    RES_480P("480p SD", 854, 480);

    val size: Size get() = Size(width, height)
}

enum class StreamFramerate(val label: String, val fps: Int) {
    FPS_24("24 fps", 24),
    FPS_30("30 fps", 30),
    FPS_60("60 fps", 60)
}

data class CameraSettings(
    val lens: CameraLens = CameraLens.BACK,
    val targetResolution: StreamResolution = StreamResolution.RES_720P,
    val targetFramerate: StreamFramerate = StreamFramerate.FPS_30,
    val jpegQuality: Int = 80,
    val isTorchEnabled: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,
    val exposureCompensation: Int = 0,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val keepScreenOn: Boolean = true
)
