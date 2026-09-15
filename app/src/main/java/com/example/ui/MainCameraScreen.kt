package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.CameraManager
import com.example.ui.components.CameraControls
import com.example.ui.components.ConnectionPanel
import com.example.ui.components.FocusRing
import com.example.ui.components.HotspotHelpDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StreamStatusBar
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MainCameraScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val cameraSettings by viewModel.cameraSettings.collectAsStateWithLifecycle()
    val streamStats by viewModel.streamStats.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var showConnectionPanel by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHotspotHelp by remember { mutableStateOf(false) }

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Keep screen awake handling
    DisposableEffect(cameraSettings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (cameraSettings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle user messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Permission Checking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // CameraManager setup
    val cameraManager = remember {
        CameraManager(context) { jpegBytes, width, height ->
            viewModel.onFrameAvailable(jpegBytes, width, height)
        }.also {
            viewModel.initCameraManager(it)
        }
    }

    // Update settings in CameraManager when cameraSettings change
    LaunchedEffect(cameraSettings, previewViewRef, hasCameraPermission) {
        val pv = previewViewRef
        if (pv != null && hasCameraPermission) {
            cameraManager.updateSettings(lifecycleOwner, pv, cameraSettings)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                // Fullscreen Camera Preview View
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            previewViewRef = this
                            cameraManager.initialize(
                                lifecycleOwner = lifecycleOwner,
                                previewView = this,
                                settings = cameraSettings
                            ) { success ->
                                if (success) {
                                    // Auto-start streaming if desirable, or stay standby
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Pinch to zoom gesture
                            detectTransformGestures { _, _, zoom, _ ->
                                val newZoom = cameraSettings.zoomRatio * zoom
                                viewModel.setZoomRatio(newZoom)
                            }
                        }
                        .pointerInput(Unit) {
                            // Tap to focus gesture
                            detectTapGestures { offset ->
                                focusPoint = offset
                                previewViewRef?.let { pv ->
                                    cameraManager.focusOnPoint(
                                        offset.x,
                                        offset.y,
                                        pv.width.toFloat(),
                                        pv.height.toFloat()
                                    )
                                }
                            }
                        }
                )

                // Visual Tap-to-Focus Ring
                FocusRing(
                    focusPoint = focusPoint,
                    onFocusAnimationFinished = { focusPoint = null }
                )

                // Top Status Bar (HUD)
                StreamStatusBar(
                    connectionState = connectionState,
                    cameraSettings = cameraSettings,
                    streamStats = streamStats,
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenConnectionPanel = { showConnectionPanel = !showConnectionPanel },
                    onOpenHotspotHelp = { showHotspotHelp = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                // Bottom Camera & Streaming Controls
                CameraControls(
                    cameraSettings = cameraSettings,
                    connectionState = connectionState,
                    onToggleStreaming = { viewModel.toggleStreaming() },
                    onSnapPhoto = { viewModel.snapPhoto() },
                    onToggleTorch = { viewModel.toggleTorch() },
                    onSwitchLens = { viewModel.switchCameraLens() },
                    onSelectZoomRatio = { viewModel.setZoomRatio(it) },
                    onOpenConnectionPanel = { showConnectionPanel = !showConnectionPanel },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Expandable Connection Panel (Drawer Overlay)
                AnimatedVisibility(
                    visible = showConnectionPanel,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ConnectionPanel(
                        connectionState = connectionState,
                        onClose = { showConnectionPanel = false },
                        onOpenHotspotHelp = {
                            showConnectionPanel = false
                            showHotspotHelp = true
                        },
                        onShowToast = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }

            } else {
                // Permission Denied View
                CameraPermissionDeniedView(
                    onRequestPermission = {
                        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }

            // Dialogs
            if (showSettingsDialog) {
                SettingsDialog(
                    settings = cameraSettings,
                    onSettingsChanged = { viewModel.updateCameraSettings(it) },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (showHotspotHelp) {
                HotspotHelpDialog(
                    onDismiss = { showHotspotHelp = false },
                    onOpenHotspotSettings = {
                        context.startActivity(viewModel.openHotspotSettings())
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GunmetalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera Permission Required",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "LocalCam turns your phone into a live camera for your laptop. Camera access is required to stream video over your local Wi-Fi or hotspot.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BroadcastCyan,
                        contentColor = Color(0xFF0A0D14)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
