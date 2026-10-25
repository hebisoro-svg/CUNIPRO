package com.example.cunipro.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cunipro.CuniProApplication
import com.example.cunipro.util.NotificationScheduler
import com.example.cunipro.util.TaskNotificationScheduler
import kotlinx.coroutines.launch

class AlertSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cuni_alert_settings", Context.MODE_PRIVATE)

    private val _stockFaible = MutableLiveData(prefs.getBoolean("stock_faible", true))
    val stockFaible: LiveData<Boolean> = _stockFaible

    private val _miseBas = MutableLiveData(prefs.getBoolean("mise_bas", true))
    val miseBas: LiveData<Boolean> = _miseBas

    private val _sevrage = MutableLiveData(prefs.getBoolean("sevrage", true))
    val sevrage: LiveData<Boolean> = _sevrage

    private val _soins = MutableLiveData(prefs.getBoolean("soins", true))
    val soins: LiveData<Boolean> = _soins

    private val _tachesImportantes = MutableLiveData(prefs.getBoolean("taches_importantes", true))
    val tachesImportantes: LiveData<Boolean> = _tachesImportantes

    private val _morningTime = MutableLiveData(prefs.getString("morning_time", "07:00") ?: "07:00")
    val morningTime: LiveData<String> = _morningTime

    private fun updateScheduleAndTrigger() {
        NotificationScheduler.scheduleDailyNotificationWorker(getApplication())
        NotificationScheduler.triggerImmediateCheck(getApplication())
    }

    fun setStockFaible(enabled: Boolean) {
        prefs.edit().putBoolean("stock_faible", enabled).apply()
        _stockFaible.value = enabled
        updateScheduleAndTrigger()
    }

    fun setMiseBas(enabled: Boolean) {
        prefs.edit().putBoolean("mise_bas", enabled).apply()
        _miseBas.value = enabled
        updateScheduleAndTrigger()
    }

    fun setSevrage(enabled: Boolean) {
        prefs.edit().putBoolean("sevrage", enabled).apply()
        _sevrage.value = enabled
        updateScheduleAndTrigger()
    }

    fun setSoins(enabled: Boolean) {
        prefs.edit().putBoolean("soins", enabled).apply()
        _soins.value = enabled
        updateScheduleAndTrigger()
    }

    fun setTachesImportantes(enabled: Boolean) {
        prefs.edit().putBoolean("taches_importantes", enabled).apply()
        _tachesImportantes.value = enabled
        updateScheduleAndTrigger()
        viewModelScope.launch {
            val app = getApplication<CuniProApplication>()
            TaskNotificationScheduler.rescheduleAllImportantTasks(app, app.database.taskDao())
        }
    }

    fun setMorningTime(time: String) {
        prefs.edit().putString("morning_time", time).apply()
        _morningTime.value = time
        updateScheduleAndTrigger()
    }
}
