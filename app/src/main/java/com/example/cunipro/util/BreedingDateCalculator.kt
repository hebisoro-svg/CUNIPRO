package com.example.cunipro.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object BreedingDateCalculator {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val PREFS_NAME = "cunipro_settings_prefs"

    fun getTodayString(): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.format(Date())
    }

    fun getPalpationDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("pref_palpation_days", 12)
    }

    fun getGestationDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("pref_gestation_days", 31)
    }

    fun getSevrageDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("pref_sevrage_days", 35)
    }

    /**
     * Palpation Date = Mating Date + daysOffset
     */
    fun calculatePalpationDate(matingDateStr: String, daysOffset: Int = 12): String {
        return addDaysToDate(matingDateStr, daysOffset)
    }

    /**
     * Expected Kindling Date (Date prévisionnelle Mise bas) = Mating Date + daysOffset
     */
    fun calculateExpectedKindlingDate(matingDateStr: String, daysOffset: Int = 31): String {
        return addDaysToDate(matingDateStr, daysOffset)
    }

    /**
     * Expected Weaning Date (Date prévisionnelle Sevrage) = Kindling Date + daysOffset
     */
    fun calculateExpectedWeaningDate(kindlingDateStr: String, daysOffset: Int = 35): String {
        return addDaysToDate(kindlingDateStr, daysOffset)
    }

    fun addDaysToDate(dateStr: String, days: Int): String {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val calendar = Calendar.getInstance()
            val date = sdf.parse(dateStr)
            if (date != null) {
                calendar.time = date
                calendar.add(Calendar.DAY_OF_YEAR, days)
                sdf.format(calendar.time)
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDateDisplay(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "-"
        return try {
            val inputFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
