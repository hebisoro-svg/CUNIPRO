package com.example.cunipro.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cunipro.data.repository.AuthRepository
import com.example.cunipro.data.repository.FirebaseSyncRepository
import com.example.cunipro.util.UserUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
    private val authRepository = AuthRepository(application)
    private val syncRepository = FirebaseSyncRepository(application)

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userPhone = MutableLiveData<String>()
    val userPhone: LiveData<String> = _userPhone

    private val _userFarmName = MutableLiveData<String>()
    val userFarmName: LiveData<String> = _userFarmName

    private val _userInitials = MutableLiveData<String>()
    val userInitials: LiveData<String> = _userInitials

    private val _isLanguageFr = MutableLiveData(prefs.getBoolean("pref_lang_is_fr", true))
    val isLanguageFr: LiveData<Boolean> = _isLanguageFr

    private val _syncWifiOnly = MutableLiveData(prefs.getBoolean("pref_sync_wifi_only", false))
    val syncWifiOnly: LiveData<Boolean> = _syncWifiOnly

    private val _autoCloudBackup = MutableLiveData(prefs.getBoolean("pref_auto_cloud_backup", true))
    val autoCloudBackup: LiveData<Boolean> = _autoCloudBackup

    private val _lastSyncTime = MutableLiveData(prefs.getString("pref_last_sync_time", "aujourd'hui, 07:12") ?: "aujourd'hui, 07:12")
    val lastSyncTime: LiveData<String> = _lastSyncTime

    private val _dataToSyncCount = MutableLiveData(prefs.getInt("pref_data_sync_count", 0))
    val dataToSyncCount: LiveData<Int> = _dataToSyncCount

    private val _palpationDays = MutableLiveData(prefs.getInt("pref_palpation_days", 12))
    val palpationDays: LiveData<Int> = _palpationDays

    private val _gestationDays = MutableLiveData(prefs.getInt("pref_gestation_days", 31))
    val gestationDays: LiveData<Int> = _gestationDays

    private val _sevrageDays = MutableLiveData(prefs.getInt("pref_sevrage_days", 35))
    val sevrageDays: LiveData<Int> = _sevrageDays

    private val _isSyncing = MutableLiveData(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val _syncResult = MutableLiveData<Result<Unit>?>()
    val syncResult: LiveData<Result<Unit>?> = _syncResult

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "pref_last_sync_time" || key == "pref_data_sync_count") {
            val updatedTime = sharedPreferences.getString("pref_last_sync_time", null)
            if (!updatedTime.isNullOrEmpty()) {
                _lastSyncTime.postValue(updatedTime)
            }
            val syncCount = sharedPreferences.getInt("pref_data_sync_count", -1)
            if (syncCount == 0) {
                _dataToSyncCount.postValue(0)
            } else {
                viewModelScope.launch {
                    val totalLocalRecords = syncRepository.getPendingSyncCount()
                    _dataToSyncCount.postValue(totalLocalRecords)
                }
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        refreshUserData()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun refreshUserData() {
        val firebaseUser = authRepository.currentUser
        val savedName = prefs.getString("pref_user_name", null)
            ?: firebaseUser?.displayName
            ?: "Awa Koné"

        val savedEmail = firebaseUser?.email
            ?: prefs.getString("pref_user_email", "awa.kone@exemple.com")
            ?: "awa.kone@exemple.com"

        val savedPhone = prefs.getString("pref_user_phone", "") ?: ""
        val savedFarm = prefs.getString("pref_user_farm_name", "Ferme Koné") ?: "Ferme Koné"

        _userName.value = savedName
        _userEmail.value = savedEmail
        _userPhone.value = savedPhone
        _userFarmName.value = savedFarm
        _userInitials.value = UserUtils.extractInitials(savedName)

        _isLanguageFr.value = prefs.getBoolean("pref_lang_is_fr", true)
        _syncWifiOnly.value = prefs.getBoolean("pref_sync_wifi_only", false)
        _autoCloudBackup.value = prefs.getBoolean("pref_auto_cloud_backup", true)
        _lastSyncTime.value = prefs.getString("pref_last_sync_time", "aujourd'hui, 07:12") ?: "aujourd'hui, 07:12"
        _palpationDays.value = prefs.getInt("pref_palpation_days", 12)
        _gestationDays.value = prefs.getInt("pref_gestation_days", 31)
        _sevrageDays.value = prefs.getInt("pref_sevrage_days", 35)

        viewModelScope.launch {
            val syncCountPref = prefs.getInt("pref_data_sync_count", -1)
            if (syncCountPref == 0) {
                _dataToSyncCount.value = 0
            } else {
                val totalLocalRecords = syncRepository.getPendingSyncCount()
                _dataToSyncCount.value = totalLocalRecords
            }
        }
    }

    fun setLanguageIsFr(isFr: Boolean) {
        prefs.edit().putBoolean("pref_lang_is_fr", isFr).apply()
        _isLanguageFr.value = isFr
    }

    fun setSyncWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("pref_sync_wifi_only", enabled).apply()
        _syncWifiOnly.value = enabled
    }

    fun setAutoCloudBackup(enabled: Boolean) {
        prefs.edit().putBoolean("pref_auto_cloud_backup", enabled).apply()
        _autoCloudBackup.value = enabled
    }

    fun updateBreedingDurations(palpation: Int, gestation: Int, sevrage: Int) {
        prefs.edit()
            .putInt("pref_palpation_days", palpation)
            .putInt("pref_gestation_days", gestation)
            .putInt("pref_sevrage_days", sevrage)
            .apply()

        _palpationDays.value = palpation
        _gestationDays.value = gestation
        _sevrageDays.value = sevrage
    }

    fun triggerManualSync() {
        _isSyncing.value = true
        viewModelScope.launch {
            val result = syncRepository.fullSync()
            if (result.isSuccess) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val formattedTime = "aujourd'hui, ${timeFormat.format(Date())}"
                prefs.edit().putString("pref_last_sync_time", formattedTime).apply()
                prefs.edit().putInt("pref_data_sync_count", 0).apply()

                _lastSyncTime.value = formattedTime
                _dataToSyncCount.value = 0
            }
            _syncResult.value = result
            _isSyncing.value = false
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    fun signOut() {
        authRepository.signOut()
    }
}
