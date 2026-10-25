package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tagNumber: String, // Identification code e.g. "M-01", "F-03"
    val sex: String, // "M" (Mâle) or "F" (Femelle)
    val category: String, // "REPRODUCTEUR", "ENGRAISSEMENT", "SEVRE"
    val status: String = "VIVANT", // "VIVANT", "VENDU", "DECEDE"
    val breed: String? = null, // Breed name e.g. "Fauve de Bourgogne", "NZ White", "Local"
    val birthDate: String? = null, // "YYYY-MM-DD"
    val weightGrams: Int? = null, // Weight in grams
    val cageNumber: String? = null, // Cage / Box identifier
    val origin: String? = null, // Animal origin e.g. "Achat", "Élevage local"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
