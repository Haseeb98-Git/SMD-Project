package com.haseebali.savelife.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConnectivityManager(private val context: Context) {
    private val TAG = "ConnectivityManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    // Network callback for newer Android versions
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available callback")
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost callback")
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            Log.d(TAG, "Network capabilities changed")
            updateNetworkStatus()
        }
    }

    // Broadcast receiver for older Android versions and as a fallback
    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Connectivity broadcast received: ${intent.action}")
            updateNetworkStatus()
        }
    }

    init {
        // Initialize with current network status
        updateNetworkStatus()
        
        // Register network callback for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
        
        // Register broadcast receiver for all Android versions as a fallback
        val filter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        }
        context.registerReceiver(connectivityReceiver, filter)
        
        // Log initial network status
        Log.d(TAG, "Initial network status: ${_isNetworkAvailable.value}")
    }

    private fun updateNetworkStatus() {
        val isConnected = isNetworkConnected()
        Log.d(TAG, "Network status updated: $isConnected")
        _isNetworkAvailable.value = isConnected
    }

    private fun isNetworkConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            Log.d(TAG, "Network check (API 23+): $hasInternet")
            hasInternet
        } else {
            @Suppress("DEPRECATION")
            val isConnected = connectivityManager.activeNetworkInfo?.isConnected == true
            Log.d(TAG, "Network check (legacy): $isConnected")
            isConnected
        }
    }

    fun unregisterNetworkCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
            context.unregisterReceiver(connectivityReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network listeners: ${e.message}")
        }
    }
    
    // Call this method to force a network status update
    fun refreshNetworkStatus() {
        Log.d(TAG, "Manually refreshing network status")
        updateNetworkStatus()
    }
} 