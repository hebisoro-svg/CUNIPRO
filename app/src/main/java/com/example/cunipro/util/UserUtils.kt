package com.example.cunipro.util

import java.util.Locale

object UserUtils {

    /**
     * Computes 1 to 2 uppercase initials from a full name string.
     * Example:
     * - "Awa Koné" -> "AK"
     * - "Moussa" -> "M"
     * - "" -> "CP"
     */
    fun extractInitials(fullName: String?): String {
        if (fullName.isNullOrBlank()) return "AK"
        val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.isEmpty()) return "AK"

        return if (parts.size == 1) {
            parts[0].take(1).uppercase(Locale.getDefault())
        } else {
            val firstChar = parts[0].take(1)
            val lastChar = parts[parts.size - 1].take(1)
            (firstChar + lastChar).uppercase(Locale.getDefault())
        }
    }
}
