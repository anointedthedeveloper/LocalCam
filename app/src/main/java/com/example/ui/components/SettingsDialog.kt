package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CameraSettings
import com.example.data.StreamFramerate
import com.example.data.StreamResolution
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.GunmetalSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsDialog(
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GunmetalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BroadcastCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Stream Settings",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_settings_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Resolution Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "STREAM RESOLUTION",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StreamResolution.values().forEach { res ->
                            val isSelected = settings.targetResolution == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BroadcastCyan else GunmetalSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) BroadcastCyan else GunmetalBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSettingsChanged(settings.copy(targetResolution = res)) }
                                    .padding(vertical = 10.dp)
                                    .testTag("res_${res.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res.label.substringBefore(" "),
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Framerate Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TARGET FRAME RATE",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StreamFramerate.values().forEach { fps ->
                            val isSelected = settings.targetFramerate == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BroadcastCyan else GunmetalSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) BroadcastCyan else GunmetalBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSettingsChanged(settings.copy(targetFramerate = fps)) }
                                    .padding(vertical = 10.dp)
                                    .testTag("fps_${fps.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fps.label,
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Brightness / Exposure Compensation Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAMERA BRIGHTNESS (EXPOSURE EV)",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${if (settings.exposureCompensation > 0) "+" else ""}${settings.exposureCompensation} EV",
                            color = BroadcastCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val minExp = settings.minExposureIndex.toFloat()
                    val maxExp = settings.maxExposureIndex.toFloat()
                    val steps = (settings.maxExposureIndex - settings.minExposureIndex - 1).coerceAtLeast(0)
                    Slider(
                        value = settings.exposureCompensation.toFloat(),
                        onValueChange = { onSettingsChanged(settings.copy(exposureCompensation = it.toInt())) },
                        valueRange = minExp..maxExp,
                        steps = steps,
                        colors = SliderDefaults.colors(
                            thumbColor = BroadcastCyan,
                            activeTrackColor = BroadcastCyan,
                            inactiveTrackColor = GunmetalBorder
                        )
                    )
                }

                // JPEG Quality / Adaptive Bitrate Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "STREAM COMPRESSION / BITRATE",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${settings.jpegQuality}%",
                            color = BroadcastCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.jpegQuality.toFloat(),
                        onValueChange = { onSettingsChanged(settings.copy(jpegQuality = it.toInt())) },
                        valueRange = 50f..95f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = BroadcastCyan,
                            activeTrackColor = BroadcastCyan,
                            inactiveTrackColor = GunmetalBorder
                        )
                    )
                }

                // Keep Screen Awake toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F141F))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Keep Screen On",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Prevents screen sleep during live filming",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { onSettingsChanged(settings.copy(keepScreenOn = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0A0D14),
                            checkedTrackColor = BroadcastCyan,
                            uncheckedTrackColor = GunmetalBorder
                        )
                    )
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GunmetalSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
