package com.example.cunipro.ui.tasks

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.NotificationEntity
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.data.repository.NotificationRepository
import com.example.cunipro.data.repository.TaskRepository
import kotlinx.coroutines.launch

class TasksViewModel(
    application: Application,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository
) : AndroidViewModel(application) {

    // Selected tab: 0 = Received Notifications, 1 = Tasks
    private val _selectedTab = MutableLiveData(1) // Default tab 1 ("Tâches")
    val selectedTab: LiveData<Int> = _selectedTab

    val allTasks: LiveData<List<TaskEntity>> = taskRepository.allTasks.asLiveData()
    val allNotifications: LiveData<List<NotificationEntity>> = notificationRepository.allNotifications.asLiveData()
    val unreadNotificationCount: LiveData<Int> = notificationRepository.unreadCount.asLiveData()

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("cunipro_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("demo_tasks_cleaned_v1", false)) {
                notificationRepository.deleteAllNotifications()
                taskRepository.deleteAllTasks()
                prefs.edit().putBoolean("demo_tasks_cleaned_v1", true).apply()
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun addTask(
        title: String,
        category: String,
        dueDate: String,
        dueTime: String,
        reminderInfo: String,
        repeatInfo: String?,
        isFlagged: Boolean
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = if (isFlagged) "HAUTE" else "MOYENNE",
                type = when (category.lowercase()) {
                    "vente prévue", "vente" -> "VENTE"
                    "alimentation" -> "ALIMENTATION"
                    "soin" -> "SOIN"
                    "saillie" -> "SAILLIE"
                    "sevrage" -> "SEVRAGE"
                    else -> "AUTRE"
                },
                category = category,
                reminderInfo = reminderInfo,
                repeatInfo = repeatInfo,
                isFlagged = isFlagged,
                isAuto = false
            )
            taskRepository.insertTask(task)
        }
    }

    fun markTaskCompleted(task: TaskEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.markTaskCompleted(task, isCompleted)
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
}

class TasksViewModelFactory(
    private val application: Application,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(application, taskRepository, notificationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
