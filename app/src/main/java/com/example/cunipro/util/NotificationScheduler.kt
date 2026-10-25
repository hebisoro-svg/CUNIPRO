package com.example.cunipro.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cunipro.worker.ReproductionNotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    const val WORK_NAME_PERIODIC = "cuni_daily_notification_worker"

    fun scheduleDailyNotificationWorker(context: Context) {
        val prefs = context.getSharedPreferences("cuni_alert_settings", Context.MODE_PRIVATE)
        val morningTime = prefs.getString("morning_time", "07:00") ?: "07:00"

        val timeParts = morningTime.split(":")
        val targetHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 7
        val targetMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelayMs = target.timeInMillis - now.timeInMillis

        val periodicWork = PeriodicWorkRequestBuilder<ReproductionNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWork
        )
    }

    fun triggerImmediateCheck(context: Context) {
        val immediateWork = OneTimeWorkRequestBuilder<ReproductionNotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(immediateWork)
    }
}
