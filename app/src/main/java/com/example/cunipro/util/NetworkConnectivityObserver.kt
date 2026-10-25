package com.example.cunipro.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.cunipro.data.repository.FirebaseSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NetworkConnectivityObserver {

    private var isRegistered = false

    fun registerNetworkCallback(context: Context) {
        if (isRegistered) return
        isRegistered = true

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val syncRepo = FirebaseSyncRepository(context)

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val prefs = context.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
                val autoBackup = prefs.getBoolean("pref_auto_cloud_backup", true)
                val wifiOnly = prefs.getBoolean("pref_sync_wifi_only", false)

                if (!autoBackup) return

                if (wifiOnly) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    if (!isWifi) return
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val result = syncRepo.fullSync()
                    if (result.isSuccess) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val formattedTime = "aujourd'hui, ${timeFormat.format(Date())}"
                        prefs.edit()
                            .putString("pref_last_sync_time", formattedTime)
                            .putInt("pref_data_sync_count", 0)
                            .apply()
                    }
                }
            }
        })
    }
}
