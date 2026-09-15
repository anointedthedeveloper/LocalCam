package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraLens
import com.example.data.CameraSettings
import com.example.data.ConnectionState
import com.example.data.NetworkType
import com.example.data.StreamStats
import com.example.data.StreamingStatus
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.LiveTallyRed
import com.example.ui.theme.OverlayGlass
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StreamStatusBar(
    connectionState: ConnectionState,
    cameraSettings: CameraSettings,
    streamStats: StreamStats,
    onOpenSettings: () -> Unit,
    onOpenConnectionPanel: () -> Unit,
    onOpenHotspotHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(OverlayGlass)
            .border(1.dp, GunmetalBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Row: Brand, Tally, Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Camera Lens indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LOCALCAM",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                // Lens Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2638))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (cameraSettings.lens == CameraLens.BACK) "REAR" else "FRONT",
                        color = BroadcastCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Tally Status Indicator (LIVE / STREAMING / STANDBY)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    connectionState.streamingStatus == StreamingStatus.STREAMING && connectionState.hasConnectedClients -> {
                        // Fully LIVE with laptop connected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(LiveTallyRed.copy(alpha = 0.2f))
                                .border(1.dp, LiveTallyRed, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .alpha(liveAlpha)
                                    .clip(CircleShape)
                                    .background(LiveTallyRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE • OBS",
                                color = LiveTallyRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    connectionState.streamingStatus == StreamingStatus.STREAMING -> {
                        // Streaming server ready, waiting for laptop
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(StudioAmber.copy(alpha = 0.2f))
                                .border(1.dp, StudioAmber, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StudioAmber)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "READY",
                                color = StudioAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        // Standby
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E2638))
                                .border(1.dp, GunmetalBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "STANDBY",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenHotspotHelp,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("hotspot_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Hotspot Help",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Row: Network IP, Resolution, FPS, Bitrate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Network & IP Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF121824))
                    .border(1.dp, GunmetalBorder, RoundedCornerShape(6.dp))
                    .clickable { onOpenConnectionPanel() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("network_status_pill")
            ) {
                Icon(
                    imageVector = when (connectionState.networkType) {
                        NetworkType.HOTSPOT -> Icons.Default.WifiTethering
                        NetworkType.WIFI -> Icons.Default.Wifi
                        NetworkType.DISCONNECTED -> Icons.Default.WifiTethering
                    },
                    contentDescription = "Network type",
                    tint = if (connectionState.networkType == NetworkType.DISCONNECTED) LiveTallyRed else StatusGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = connectionState.localIpAddress ?: "No Hotspot/Network",
                    color = if (connectionState.networkType == NetworkType.DISCONNECTED) LiveTallyRed else TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            // Client & Telemetry Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Resolution
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF161F2E))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = cameraSettings.targetResolution.label.substringBefore(" "),
                        color = BroadcastCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Connected Clients count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (connectionState.hasConnectedClients) StatusGreen.copy(alpha = 0.15f)
                            else Color(0xFF161F2E)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = "Clients",
                        tint = if (connectionState.hasConnectedClients) StatusGreen else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (connectionState.hasConnectedClients) "${connectionState.connectedClients.size}" else "0",
                        color = if (connectionState.hasConnectedClients) StatusGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Live FPS
                if (connectionState.streamingStatus == StreamingStatus.STREAMING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF161F2E))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = streamStats.formattedFps,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
