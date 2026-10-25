package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_distribution")
data class FeedDistributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedStockId: Long,
    val feedName: String,
    val distributionDate: String, // "YYYY-MM-DD"
    val quantityKg: Double, // Quantité distribuée en kg
    val targetCategory: String = "TOUS", // "TOUS", "REPRODUCTEUR", "ENGRAISSEMENT"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
