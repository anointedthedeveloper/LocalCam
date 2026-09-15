package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraSettings
import com.example.data.ConnectionState
import com.example.data.StreamingStatus
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.GunmetalSurfaceElevated
import com.example.ui.theme.LiveTallyRed
import com.example.ui.theme.OverlayGlass
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CameraControls(
    cameraSettings: CameraSettings,
    connectionState: ConnectionState,
    onToggleStreaming: () -> Unit,
    onSnapPhoto: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchLens: () -> Unit,
    onSelectZoomRatio: (Float) -> Unit,
    onOpenConnectionPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapInteractionSource = remember { MutableInteractionSource() }
    val isSnapPressed by snapInteractionSource.collectIsPressedAsState()
    val snapScale by animateFloatAsState(
        targetValue = if (isSnapPressed) 0.88f else 1.0f,
        label = "snapScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(OverlayGlass)
            .border(1.dp, GunmetalBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zoom Presets Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val presets = listOf(
                1.0f to "1x",
                2.0f to "2x",
                cameraSettings.maxZoomRatio.coerceAtMost(5.0f) to "${cameraSettings.maxZoomRatio.toInt()}x"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141A26))
                    .border(1.dp, GunmetalBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { (ratio, label) ->
                    val isSelected = kotlin.math.abs(cameraSettings.zoomRatio - ratio) < 0.15f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BroadcastCyan else Color.Transparent)
                            .clickable { onSelectZoomRatio(ratio) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("zoom_preset_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkObsidian else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Current Zoom display
                Text(
                    text = String.format(Locale.US, "%.1fx", cameraSettings.zoomRatio),
                    color = BroadcastCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Primary Control Deck: Side Tools + Big Snap + Stream Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Torch & Lens Switch
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle
                IconButton(
                    onClick = onToggleTorch,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (cameraSettings.isTorchEnabled) BroadcastCyan.copy(alpha = 0.2f) else GunmetalSurfaceElevated,
                        contentColor = if (cameraSettings.isTorchEnabled) BroadcastCyan else TextPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            1.dp,
                            if (cameraSettings.isTorchEnabled) BroadcastCyan else GunmetalBorder,
                            CircleShape
                        )
                        .testTag("torch_button")
                ) {
                    Icon(
                        imageVector = if (cameraSettings.isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch Toggle"
                    )
                }

                // Lens Flip
                IconButton(
                    onClick = onSwitchLens,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = GunmetalSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, GunmetalBorder, CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera"
                    )
                }
            }

            // Center: Dedicated Shutter "SNAP" Button for full-res photo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(snapScale)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(3.dp, Color.White, CircleShape)
                    .padding(5.dp)
                    .clickable(
                        interactionSource = snapInteractionSource,
                        indication = null,
                        onClick = onSnapPhoto
                    )
                    .testTag("snap_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Right Group: Connection info & Streaming Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link / Laptop Drawer button
                IconButton(
                    onClick = onOpenConnectionPanel,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (connectionState.hasConnectedClients) StatusGreen.copy(alpha = 0.2f) else GunmetalSurfaceElevated,
                        contentColor = if (connectionState.hasConnectedClients) StatusGreen else TextPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            1.dp,
                            if (connectionState.hasConnectedClients) StatusGreen else GunmetalBorder,
                            CircleShape
                        )
                        .testTag("connection_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Connection Info"
                    )
                }

                // Master Stream Toggle Button
                val isStreaming = connectionState.streamingStatus == StreamingStatus.STREAMING
                Button(
                    onClick = onToggleStreaming,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStreaming) LiveTallyRed else StatusGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("stream_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isStreaming) "STOP" else "STREAM",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
