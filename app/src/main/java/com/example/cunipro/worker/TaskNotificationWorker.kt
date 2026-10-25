package com.example.cunipro.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cunipro.CuniProApplication
import com.example.cunipro.data.local.entity.NotificationEntity
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.NotificationHelper

class TaskNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? CuniProApplication ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("cuni_alert_settings", Context.MODE_PRIVATE)

        val isTachesImportantesEnabled = prefs.getBoolean("taches_importantes", true)
        if (!isTachesImportantesEnabled) {
            return Result.success()
        }

        val taskId = inputData.getLong("task_id", 0L)
        val isFr = LanguageManager.isFrench(applicationContext)
        val defaultTaskStr = if (isFr) "Tâche" else "Task"
        val taskTitle = inputData.getString("task_title") ?: defaultTaskStr
        val taskTime = inputData.getString("task_time") ?: "08:00"

        val title = if (isFr) "Tâche importante : $taskTitle 📋" else "Important Task: $taskTitle 📋"
        val message = if (isFr) "Rappel pour la tâche '$taskTitle' prévue à $taskTime." else "Reminder for task '$taskTitle' scheduled at $taskTime."
        val todayStr = BreedingDateCalculator.getTodayString()

        NotificationHelper.sendNotification(
            context = applicationContext,
            notificationId = 4000 + taskId.toInt(),
            title = title,
            message = message
        )

        application.database.notificationDao().insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = "TASK",
                date = todayStr,
                time = taskTime,
                isRead = false
            )
        )

        return Result.success()
    }
}
