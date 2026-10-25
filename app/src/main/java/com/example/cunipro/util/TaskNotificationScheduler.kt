package com.example.cunipro.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.cunipro.data.local.dao.TaskDao
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.worker.TaskNotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskNotificationScheduler {

    fun scheduleTaskNotification(context: Context, task: TaskEntity) {
        if (task.isCompleted) return

        val isImportant = task.isFlagged || task.priority.equals("HAUTE", ignoreCase = true)
        if (!isImportant) return

        val targetMs = parseTaskDateTimeToMillis(task.dueDate, task.dueTime) ?: return
        val nowMs = System.currentTimeMillis()

        if (targetMs <= nowMs) return

        val initialDelayMs = targetMs - nowMs

        val inputData: Data = workDataOf(
            "task_id" to task.id,
            "task_title" to task.title,
            "task_time" to task.dueTime
        )

        val workRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_notification_${task.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelTaskNotification(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("task_notification_$taskId")
    }

    suspend fun rescheduleAllImportantTasks(context: Context, taskDao: TaskDao) {
        val todayStr = BreedingDateCalculator.getTodayString()
        val pendingTasks = taskDao.getPendingTasksDueByList(todayStr)
        for (task in pendingTasks) {
            scheduleTaskNotification(context, task)
        }
    }

    private fun parseTaskDateTimeToMillis(dateStr: String, timeStr: String): Long? {
        val dateParts = dateStr.split("-")
        if (dateParts.size < 3) return null
        val year = dateParts[0].toIntOrNull() ?: return null
        val month = (dateParts[1].toIntOrNull() ?: return null) - 1
        val day = dateParts[2].toIntOrNull() ?: return null

        val timeParts = timeStr.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis
    }
}
