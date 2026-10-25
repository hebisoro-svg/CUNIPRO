package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: Long? = null,
    val animalTag: String? = null, // "L-021 · Californien", "Portée L-019", "Tout l'élevage"
    val diseaseName: String = "", // e.g. "Coccidiose", "Gale des oreilles", "Vaccin myxomatose", "Diarrhée"
    val symptoms: String? = null, // e.g. "Diarrhée, perte d'appétit"
    val treatmentType: String = "TRAITEMENT", // "VACCIN", "TRAITEMENT", "DESINFECTION", "MALADIE"
    val treatmentName: String = "", // e.g. "Anticoccidien", "Traitement local", "Vaccin"
    val dosage: String? = null, // e.g. "1 dose par jour"
    val status: String = "EN_COURS", // "EN_COURS", "TERMINE"
    val treatmentDate: String, // "YYYY-MM-DD" or "DD/MM/YYYY"
    val nextDueDate: String? = null, // Date rappel
    val isMortality: Boolean = false, // true for death declarations
    val deathCount: Int = 1,
    val deathCause: String? = null, // "Maladie", "Mort-né", "Chaleur", "Accident", "Autre"
    val cost: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
