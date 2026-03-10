package com.airobot.assistant.comm.transport

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监控系统网络状态的管理器,物理链路监控
 */
@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context) {
    private val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(checkCurrentNetwork())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(networkRequest,
            object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("ConnectivityMonitor", "网络可用")
                _isNetworkAvailable.value = true
            }

            override fun onLost(network: Network) {
                Log.d("ConnectivityMonitor", "网络丢失")
                _isNetworkAvailable.value = checkCurrentNetwork()
            }

            override fun onCapabilitiesChanged(network: Network,
                                               networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET)
                Log.d("ConnectivityMonitor", "网络能力变化: hasInternet = $hasInternet")
                _isNetworkAvailable.value = hasInternet
            }
        })
    }

    private fun checkCurrentNetwork(): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
