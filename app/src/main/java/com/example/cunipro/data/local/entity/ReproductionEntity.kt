package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reproductions")
data class ReproductionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val femaleId: Long,
    val femaleTag: String,
    val maleId: Long? = null,
    val maleTag: String? = null,
    val matingDate: String, // Date de saillie "YYYY-MM-DD"
    val palpationDate: String? = null, // Saillie + 10 à 12 jours
    val palpationResult: Boolean? = null, // true = Positive (Gestante), false = Négative
    val expectedKindlingDate: String? = null, // Saillie + 31 jours (Date prévisionnelle mise bas)
    val actualKindlingDate: String? = null, // Date réelle de mise bas
    val bornAliveCount: Int = 0, // Vants nés vivants
    val bornDeadCount: Int = 0, // Mort-nés
    val expectedWeaningDate: String? = null, // Mise bas + 35 jours (Date prévisionnelle sevrage)
    val actualWeaningDate: String? = null, // Date réelle de sevrage
    val weanedCount: Int = 0, // Nombre de lapereaux sevrés
    val status: String = "EN_COURS", // "EN_COURS", "TERMINE", "ECHOUE"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
