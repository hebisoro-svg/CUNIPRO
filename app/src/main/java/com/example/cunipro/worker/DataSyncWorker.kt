package com.example.cunipro.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cunipro.data.repository.FirebaseSyncRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
        val autoBackupEnabled = prefs.getBoolean("pref_auto_cloud_backup", true)

        if (!autoBackupEnabled) {
            return Result.success()
        }

        val syncRepository = FirebaseSyncRepository(applicationContext)
        val syncResult = syncRepository.fullSync()

        return if (syncResult.isSuccess) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = "aujourd'hui, ${timeFormat.format(Date())}"
            prefs.edit().putString("pref_last_sync_time", formattedTime).apply()
            prefs.edit().putInt("pref_data_sync_count", 0).apply()
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "cuni_data_sync_worker"

        fun schedulePeriodicSync(context: Context) {
            val prefs = context.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
            val wifiOnly = prefs.getBoolean("pref_sync_wifi_only", false)

            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }
    }
}
