package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_stock")
data class FeedStockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedName: String, // e.g. "Granulés Croissance", "Foin", "Son de blé"
    val quantityKg: Double, // Quantité en kg
    val minThresholdKg: Double = 10.0, // Seuil d'alerte stock bas
    val unitCost: Double = 0.0, // Prix par kg en FCFA / EUR
    val lastUpdated: Long = System.currentTimeMillis()
)
