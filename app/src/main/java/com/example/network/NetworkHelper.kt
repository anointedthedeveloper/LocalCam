package com.example.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import com.example.data.NetworkType
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class NetworkHelper(private val context: Context) {

    data class NetworkInfo(
        val type: NetworkType,
        val ipAddress: String?,
        val interfaceName: String?
    )

    fun getActiveNetworkInfo(): NetworkInfo {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Pass 1: Look for Hotspot / Access Point interfaces (ap0, softap, swlan, etc.)
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                val isHotspotInterface = name.contains("ap") ||
                        name.contains("softap") ||
                        name.contains("swlan") ||
                        name.contains("tether") ||
                        name.contains("rndis")

                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (isHotspotInterface || hostAddress.startsWith("192.168.43.")) {
                            return NetworkInfo(
                                type = NetworkType.HOTSPOT,
                                ipAddress = hostAddress,
                                interfaceName = intf.name
                            )
                        }
                    }
                }
            }

            // Pass 2: Look for Wi-Fi interface (wlan0, etc.)
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                if (name.contains("wlan") || name.contains("wifi")) {
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val hostAddress = addr.hostAddress ?: continue
                            val isHotspotIp = hostAddress.startsWith("192.168.43.")
                            return NetworkInfo(
                                type = if (isHotspotIp) NetworkType.HOTSPOT else NetworkType.WIFI,
                                ipAddress = hostAddress,
                                interfaceName = intf.name
                            )
                        }
                    }
                }
            }

            // Pass 3: Any non-loopback IPv4 interface
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        return NetworkInfo(
                            type = NetworkType.WIFI,
                            ipAddress = hostAddress,
                            interfaceName = intf.name
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return NetworkInfo(
            type = NetworkType.DISCONNECTED,
            ipAddress = null,
            interfaceName = null
        )
    }

    fun openHotspotSettings(): Intent {
        val tetherIntent = Intent().apply {
            action = "android.settings.TETHER_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (context.packageManager.queryIntentActivities(tetherIntent, 0).isNotEmpty()) {
            tetherIntent
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun observeNetworkChanges(onNetworkChanged: () -> Unit): () -> Unit {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return {}

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkChanged()
            }

            override fun onLost(network: Network) {
                onNetworkChanged()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                onNetworkChanged()
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                // Ignore unregister errors
            }
        }
    }
}
