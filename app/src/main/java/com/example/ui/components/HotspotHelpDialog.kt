package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HotspotHelpDialog(
    onDismiss: () -> Unit,
    onOpenHotspotSettings: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("1. Hotspot", Icons.Default.WifiTethering),
        TabItem("2. PC Webcam", Icons.Default.Computer),
        TabItem("3. OBS & Web", Icons.Default.Podcasts),
        TabItem("4. Troubleshooting", Icons.Default.HelpOutline)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GunmetalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = BroadcastCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "LocalCam Setup Guide",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_hotspot_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "Complete guide to stream wirelessly to your laptop without internet or mobile data.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF0F141F),
                    contentColor = BroadcastCyan,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BroadcastCyan,
                            height = 2.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, GunmetalBorder, RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) BroadcastCyan else TextSecondary
                                )
                            },
                            modifier = Modifier.testTag("setup_tab_$index")
                        )
                    }
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F141F))
                        .border(1.dp, GunmetalBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTabIndex) {
                        0 -> HotspotTabContent(onOpenHotspotSettings)
                        1 -> PcWebcamTabContent()
                        2 -> ObsTabContent()
                        3 -> TroubleshootingTabContent()
                    }
                }

                // Footer action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenHotspotSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2638),
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("open_hotspot_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = StudioAmber
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hotspot Settings",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BroadcastCyan,
                            contentColor = Color(0xFF0A0D14)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("got_it_button")
                    ) {
                        Text(
                            text = "Got It",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private data class TabItem(val title: String, val icon: ImageVector)

@Composable
private fun HotspotTabContent(onOpenHotspotSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "⚡ 100% OFFLINE / ZERO DATA USAGE",
            color = StudioAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "LocalCam connects directly over your phone's Wi-Fi hotspot. You do not need an active SIM card, cellular data, or internet connection.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        StepItem(step = "1", text = "Turn ON Personal Hotspot in your phone's Android Settings.")
        StepItem(step = "2", text = "Mobile Data can stay OFF. The local Wi-Fi link works without internet.")
        StepItem(step = "3", text = "On your laptop, connect Wi-Fi to your phone's Hotspot.")
        StepItem(step = "4", text = "Tap 'START STREAM' in LocalCam to turn on the camera server.")
        StepItem(step = "5", text = "Phone IP is usually 192.168.43.1. Enter that URL in your PC browser or client.")
    }
}

@Composable
private fun PcWebcamTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "💻 USE IN VIDEOPSALM, GOOGLE MEET, ZOOM & TEAMS",
            color = BroadcastCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Use your phone as a high-definition webcam on your PC. No OBS Studio is required!",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        StepItem(step = "1", text = "Connect laptop to the phone's Hotspot or same Wi-Fi.")
        StepItem(step = "2", text = "Open laptop browser to http://192.168.43.1:8080/pc to download the client files.")
        StepItem(step = "3", text = "WITHOUT OBS: Run 'install_virtual_camera.bat' as Administrator once to register the driver.")
        StepItem(step = "4", text = "Double-click 'start_pc_client.bat' and click 'START WEBCAM FEED'.")
        StepItem(step = "5", text = "In VideoPsalm, Google Meet, or Zoom video settings, select: 'Unity Video Capture' (or OBS Virtual Camera)!")
    }
}

@Composable
private fun ObsTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "🎥 OBS STUDIO & WEB BROWSER STREAM",
            color = BroadcastCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Embed your camera feed directly into OBS Studio for live streaming or video recording:",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        StepItem(step = "1", text = "In OBS Studio, click Sources '+' -> 'Browser'.")
        StepItem(step = "2", text = "Set URL to http://<phone-ip>:8080/obs (clean borderless video).")
        StepItem(step = "3", text = "Set Width: 1280 and Height: 720 (or matching your phone resolution).")
        StepItem(step = "4", text = "For direct MJPEG: add Media Source -> uncheck 'Local File' -> enter http://<phone-ip>:8080/video.")
        StepItem(step = "5", text = "Or open http://<phone-ip>:8080 in Chrome/Safari to watch live with full remote controls.")
    }
}

@Composable
private fun TroubleshootingTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "🛠 TROUBLESHOOTING & OPTIMIZATION",
            color = StudioAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        StepItem(step = "•", text = "Cannot connect: Ensure both phone and PC are on the exact same Wi-Fi or Hotspot network.")
        StepItem(step = "•", text = "Firewall: If PC browser cannot load, check that Windows Firewall permits port 8080.")
        StepItem(step = "•", text = "Lowest latency: Phone Hotspot provides the fastest latency (<50ms) compared to home routers.")
        StepItem(step = "•", text = "Screen sleep: Keep 'Keep Screen Awake' enabled in LocalCam Settings (gear icon) so the camera doesn't pause.")
        StepItem(step = "•", text = "Battery saving: Turn phone screen brightness down while streaming to save battery and reduce heat.")
    }
}

@Composable
private fun StepItem(step: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BroadcastCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = BroadcastCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

