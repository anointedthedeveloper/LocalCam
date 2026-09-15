package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.CameraLens
import com.example.data.CameraSettings
import com.example.data.StreamResolution
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val onFrameCaptured: (ByteArray, Int, Int) -> Unit
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var currentSettings = CameraSettings()
    private var lastFrameTimeNs = 0L

    var onZoomStateChanged: ((Float, Float, Float) -> Unit)? = null
    var onExposureStateChanged: ((Int, Int, Int) -> Unit)? = null
    var onTorchAvailabilityChanged: ((Boolean) -> Unit)? = null
    var onPhotoCaptureSuccess: ((Uri) -> Unit)? = null
    var onPhotoCaptureError: ((String) -> Unit)? = null

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: CameraSettings,
        onInitialized: (Boolean) -> Unit
    ) {
        currentSettings = settings
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onInitialized(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraProvider", e)
                onInitialized(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun updateSettings(lifecycleOwner: LifecycleOwner, previewView: PreviewView, newSettings: CameraSettings) {
        val requiresRebind = newSettings.lens != currentSettings.lens ||
                newSettings.targetResolution != currentSettings.targetResolution

        val oldTorch = currentSettings.isTorchEnabled
        currentSettings = newSettings

        if (requiresRebind) {
            bindCameraUseCases(lifecycleOwner, previewView)
        } else {
            // Apply zoom
            camera?.cameraControl?.setZoomRatio(newSettings.zoomRatio)
            // Apply torch if changed
            if (oldTorch != newSettings.isTorchEnabled) {
                camera?.cameraControl?.enableTorch(newSettings.isTorchEnabled)
            }
            // Apply exposure
            camera?.cameraControl?.setExposureCompensationIndex(newSettings.exposureCompensation)
        }
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return

        try {
            provider.unbindAll()

            val cameraSelector = if (currentSettings.lens == CameraLens.BACK) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Target resolution strategy with graceful fallback
            val targetSize = currentSettings.targetResolution.size
            val resStrategy = ResolutionStrategy(
                targetSize,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(resStrategy)
                .build()

            // Preview
            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Image Analysis for streaming
            imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            // Image Capture for full-res "Snap" photos to gallery
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(95)
                .build()

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )

            // Setup camera observation
            observeCameraState()

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun observeCameraState() {
        val cam = camera ?: return

        cam.cameraInfo.zoomState.observeForever { zoomState ->
            zoomState?.let {
                onZoomStateChanged?.invoke(it.zoomRatio, it.minZoomRatio, it.maxZoomRatio)
            }
        }

        val hasFlash = cam.cameraInfo.hasFlashUnit()
        onTorchAvailabilityChanged?.invoke(hasFlash)

        val exposureState = cam.cameraInfo.exposureState
        onExposureStateChanged?.invoke(
            exposureState.exposureCompensationIndex,
            exposureState.exposureCompensationRange.lower,
            exposureState.exposureCompensationRange.upper
        )
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val targetFps = currentSettings.targetFramerate.fps
        val minIntervalNs = 1_000_000_000L / targetFps
        val nowNs = System.nanoTime()

        // Frame rate limiter
        if (nowNs - lastFrameTimeNs < minIntervalNs) {
            imageProxy.close()
            return
        }
        lastFrameTimeNs = nowNs

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, currentSettings.jpegQuality, outputStream)
            val jpegBytes = outputStream.toByteArray()

            onFrameCaptured(jpegBytes, finalBitmap.width, finalBitmap.height)

            if (finalBitmap != bitmap) {
                finalBitmap.recycle()
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            imageProxy.close()
        }
    }

    fun snapPhoto() {
        val capture = imageCapture ?: run {
            onPhotoCaptureError?.invoke("Camera not ready for photo capture")
            return
        }

        val name = "LocalCam_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LocalCam")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && savedUri != null) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(savedUri, updateValues, null, null)
                    }

                    if (savedUri != null) {
                        onPhotoCaptureSuccess?.invoke(savedUri)
                    } else {
                        onPhotoCaptureSuccess?.invoke(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onPhotoCaptureError?.invoke(exception.message ?: "Photo capture failed")
                }
            }
        )
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun toggleTorch(enable: Boolean) {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(enable)
        }
    }

    fun setExposureIndex(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun focusOnPoint(x: Float, y: Float, width: Float, height: Float) {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(width, height)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
        } catch (ignored: Exception) {}
        camera = null
        preview = null
        imageAnalysis = null
        imageCapture = null
        cameraExecutor.shutdown()
    }
}
