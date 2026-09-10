package com.devpro58.hnem06.moneysnap.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reports whether the device currently has validated internet access.
 *
 * "Validated" matters: [NetworkCapabilities.NET_CAPABILITY_VALIDATED] is what distinguishes a real
 * connection from a captive-portal Wi-Fi that is associated but cannot reach the internet — the
 * exact situation where sync silently fails while the UI claims to be online.
 *
 * This is the only Android-facing piece; the app consumes it through `ConnectivityRepository`
 * so the domain layer stays free of platform types.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun observeIsOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            // No ConnectivityManager: assume online rather than showing a permanent offline
            // banner the user can do nothing about.
            trySend(true)
            close()
            return@callbackFlow
        }

        fun pushCurrentState() {
            val capabilities = connectivityManager.activeNetwork
                ?.let(connectivityManager::getNetworkCapabilities)
            trySend(capabilities.isOnline())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = pushCurrentState()
            override fun onLost(network: Network) = pushCurrentState()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = pushCurrentState()
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        // The callback only fires on change, so seed the current value for subscribers that
        // start while the network state is already settled.
        pushCurrentState()

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    private fun NetworkCapabilities?.isOnline(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
