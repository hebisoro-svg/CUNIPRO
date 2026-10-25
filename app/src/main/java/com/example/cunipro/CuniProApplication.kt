package com.example.cunipro

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.cunipro.data.local.CuniProDatabase
import com.example.cunipro.data.repository.*
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.NotificationHelper
import com.example.cunipro.util.NotificationScheduler

class CuniProApplication : Application() {

    val database by lazy { CuniProDatabase.getDatabase(this) }

    val animalRepository by lazy { AnimalRepository(database.animalDao()) }
    val reproductionRepository by lazy { ReproductionRepository(this, database.reproductionDao(), database.taskDao()) }
    val feedRepository by lazy { FeedRepository(this, database.feedDao(), database.notificationDao()) }
    val healthRepository by lazy { HealthRepository(database.healthDao(), database.taskDao(), database.animalDao()) }
    val financeRepository by lazy { FinanceRepository(database.financeDao()) }
    val taskRepository by lazy { TaskRepository(this, database.taskDao()) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val authRepository by lazy { AuthRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // Force Light Mode for consistent UI
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Apply saved language locale
        LanguageManager.applySavedLanguage(this)

        // Create system notification channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule background notification worker according to morning_time setting
        NotificationScheduler.scheduleDailyNotificationWorker(this)
        NotificationScheduler.triggerImmediateCheck(this)

        // Register automatic network connectivity listener for auto cloud sync
        com.example.cunipro.util.NetworkConnectivityObserver.registerNetworkCallback(this)

        // Schedule periodic background data sync worker
        com.example.cunipro.worker.DataSyncWorker.schedulePeriodicSync(this)
    }
}
