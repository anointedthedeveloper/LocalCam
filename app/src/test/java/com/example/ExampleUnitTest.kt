package com.example

import com.example.data.CameraLens
import com.example.data.CameraSettings
import com.example.data.ConnectionState
import com.example.data.NetworkType
import com.example.data.StreamFramerate
import com.example.data.StreamResolution
import com.example.data.StreamStats
import com.example.data.StreamingStatus
import com.example.server.WebReceiverHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun connectionState_streamUrl_formatsCorrectly() {
    val state = ConnectionState(
      localIpAddress = "192.168.43.1",
      port = 8080,
      networkType = NetworkType.HOTSPOT,
      streamingStatus = StreamingStatus.STREAMING
    )
    assertEquals("http://192.168.43.1:8080", state.streamUrl)
    assertFalse(state.hasConnectedClients)
  }

  @Test
  fun cameraSettings_defaultValues_areValid() {
    val settings = CameraSettings()
    assertEquals(CameraLens.BACK, settings.lens)
    assertEquals(StreamResolution.RES_720P, settings.targetResolution)
    assertEquals(StreamFramerate.FPS_30, settings.targetFramerate)
    assertEquals(1.0f, settings.zoomRatio, 0.001f)
    assertFalse(settings.isTorchEnabled)
    assertTrue(settings.keepScreenOn)
  }

  @Test
  fun streamStats_formatting_isAccurate() {
    val stats = StreamStats(
      currentFps = 29.8f,
      currentBitrateKbps = 2450L
    )
    assertEquals("29.8 fps", stats.formattedFps)
    assertEquals("2.5 Mbps", stats.formattedBitrate)
  }

  @Test
  fun webReceiverHtml_containsVitalEndpointsAndElements() {
    val html = WebReceiverHtml.getReceiverHtml("http://192.168.43.1:8080")
    assertTrue(html.contains("/video"))
    assertTrue(html.contains("/snapshot.jpg"))
    assertTrue(html.contains("/api/status"))
    assertTrue(html.contains("LocalCam"))
    assertTrue(html.contains("/obs"))

    val obsHtml = WebReceiverHtml.getObsHtml()
    assertTrue(obsHtml.contains("/video"))
    assertTrue(obsHtml.contains("overflow: hidden"))
  }

  @Test
  fun pcClientDistribution_containsKeyScriptsAndCommands() {
    val pyScript = com.example.server.PcClientDistribution.LOCALCAM_PC_CLIENT_PY
    assertTrue(pyScript.contains("LocalCam PC Virtual Webcam Client"))
    assertTrue(pyScript.contains("pyvirtualcam"))
    assertTrue(pyScript.contains("/stream.mjpg"))
    assertTrue(pyScript.contains("/api/control"))

    val batScript = com.example.server.PcClientDistribution.START_PC_CLIENT_BAT
    assertTrue(batScript.contains("localcam_pc_client.py"))
    assertTrue(batScript.contains("requirements.txt"))

    val shScript = com.example.server.PcClientDistribution.START_PC_CLIENT_SH
    assertTrue(shScript.contains("localcam_pc_client.py"))
    assertTrue(shScript.contains("requirements.txt"))

    val instructions = com.example.server.PcClientDistribution.getPcInstructionsHtml("http://192.168.43.1:8080")
    assertTrue(instructions.contains("LocalCam PC Virtual Webcam"))
    assertTrue(instructions.contains("/client/start_pc_client.bat"))
    assertTrue(instructions.contains("/client/localcam_pc_client.py"))
  }
}

