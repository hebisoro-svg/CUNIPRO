package com.example.cunipro.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cunipro.data.repository.AuthRepository
import com.example.cunipro.util.UserUtils
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
    private val authRepository = AuthRepository(application)

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

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    init {
        loadData()
    }

    fun loadData() {
        val firebaseUser = authRepository.currentUser
        val name = prefs.getString("pref_user_name", null)
            ?: firebaseUser?.displayName
            ?: "Awa Koné"

        val email = firebaseUser?.email
            ?: prefs.getString("pref_user_email", "awa.kone@exemple.com")
            ?: "awa.kone@exemple.com"

        val phone = prefs.getString("pref_user_phone", "") ?: ""
        val farm = prefs.getString("pref_user_farm_name", "Ferme Koné") ?: "Ferme Koné"

        _userName.value = name
        _userEmail.value = email
        _userPhone.value = phone
        _userFarmName.value = farm
        _userInitials.value = UserUtils.extractInitials(name)
    }

    fun computeInitials(name: String): String {
        return UserUtils.extractInitials(name)
    }

    fun saveProfile(name: String, phone: String, farmName: String) {
        viewModelScope.launch {
            prefs.edit()
                .putString("pref_user_name", name)
                .putString("pref_user_phone", phone)
                .putString("pref_user_farm_name", farmName)
                .apply()

            val firebaseUser = authRepository.currentUser
            if (firebaseUser != null && name.isNotBlank()) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    firebaseUser.updateProfile(profileUpdates)
                } catch (_: Exception) {}
            }

            _userName.value = name
            _userPhone.value = phone
            _userFarmName.value = farmName
            _userInitials.value = UserUtils.extractInitials(name)
            _saveSuccess.value = true
        }
    }
}
