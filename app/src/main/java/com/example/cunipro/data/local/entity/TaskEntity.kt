package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dueDate: String, // "YYYY-MM-DD"
    val dueTime: String = "08:00",
    val priority: String = "MOYENNE", // "HAUTE", "MOYENNE", "BASSE"
    val type: String = "AUTRE", // "PALPATION", "MISE_BAS", "SEVRAGE", "VACCIN", "AUTRE"
    val category: String? = null, // "Vente prévue", "Alimentation", "Soin", "Saillie", "Sevrage", etc.
    val reminderInfo: String = "le jour même", // "le jour même", "la veille", "2 jours avant", "3 jours avant"
    val repeatInfo: String? = null, // "chaque jour", "chaque semaine", etc.
    val isAuto: Boolean = false,
    val isFlagged: Boolean = false,
    val relatedEntityId: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
