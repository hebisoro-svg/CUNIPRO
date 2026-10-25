package com.example.cunipro.data.repository

import android.content.Context
import com.example.cunipro.data.local.dao.TaskDao
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.util.TaskNotificationScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val context: Context,
    val taskDao: TaskDao
) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val pendingTaskCount: Flow<Int> = taskDao.getPendingTaskCount()

    fun getPendingTasksForDate(date: String): Flow<List<TaskEntity>> =
        taskDao.getPendingTasksForDate(date)

    fun getPendingTasksDueBy(date: String): Flow<List<TaskEntity>> =
        taskDao.getPendingTasksDueBy(date)

    suspend fun insertTask(task: TaskEntity): Long {
        val id = taskDao.insertTask(task)
        val created = task.copy(id = id)
        TaskNotificationScheduler.scheduleTaskNotification(context, created)
        return id
    }

    suspend fun markTaskCompleted(task: TaskEntity, isCompleted: Boolean) {
        val updated = task.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
        if (isCompleted) {
            TaskNotificationScheduler.cancelTaskNotification(context, task.id)
        } else {
            TaskNotificationScheduler.scheduleTaskNotification(context, updated)
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        TaskNotificationScheduler.cancelTaskNotification(context, task.id)
    }

    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
}
