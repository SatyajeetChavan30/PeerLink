package com.example.peerlink_v002.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.PatternMatcher
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class EspConnector(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Flow that emits true when connected to a PeerLink network
    fun connectToEspNetwork(): Flow<Boolean> = callbackFlow {
        val specifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier.Builder()
                .setSsidPattern(PatternMatcher("PeerLink", PatternMatcher.PATTERN_PREFIX))
                .build()
        } else {
            // Fallback for older devices (technically not needed if minSdk=29, but good practice)
            // But strict requirement says minSdk=29 for Specifier.
            // If minSdk < 29, we'd use WifiManager.enableNetwork which is deprecated.
            // Assuming minSdk >= 29 as established.
            WifiNetworkSpecifier.Builder()
                .setSsidPattern(PatternMatcher("PeerLink", PatternMatcher.PATTERN_PREFIX))
                .build()
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("EspConnector", "Network Available: $network")
                // Bind process to this network to ensure UDP goes through it
                connectivityManager.bindProcessToNetwork(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                Log.d("EspConnector", "Network Lost: $network")
                connectivityManager.bindProcessToNetwork(null)
                trySend(false)
            }

            override fun onUnavailable() {
                Log.d("EspConnector", "Network Unavailable")
                trySend(false)
            }
        }

        connectivityManager.requestNetwork(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    fun disconnect() {
        // Unbinding is handled in awaitClose when the flow cancellation happens happens
        connectivityManager.bindProcessToNetwork(null)
    }
}
