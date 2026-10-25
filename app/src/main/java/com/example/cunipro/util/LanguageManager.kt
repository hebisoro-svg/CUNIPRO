package com.example.cunipro.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {

    private const val PREFS_NAME = "cunipro_settings_prefs"
    private const val KEY_IS_FR = "pref_lang_is_fr"

    fun setLanguage(context: Context, isFr: Boolean) {
        saveLanguagePref(context, isFr)

        val langTag = if (isFr) "fr" else "en"
        val appLocale = LocaleListCompat.forLanguageTags(langTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun saveLanguagePref(context: Context, isFr: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_FR, isFr).apply()
    }

    fun isFrench(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_FR, true)
    }

    fun getLanguageCode(context: Context): String {
        return if (isFrench(context)) "fr" else "en"
    }

    fun applySavedLanguage(context: Context) {
        val isFr = isFrench(context)
        val langTag = if (isFr) "fr" else "en"
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty || currentLocales.toLanguageTags() != langTag) {
            val appLocale = LocaleListCompat.forLanguageTags(langTag)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}
