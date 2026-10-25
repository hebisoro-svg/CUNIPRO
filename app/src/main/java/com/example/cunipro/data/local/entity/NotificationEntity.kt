package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "STOCK", "MISE_BAS", "TASK", "SOIN", "SEVRAGE", "PALPATION"
    val date: String, // "YYYY-MM-DD"
    val time: String = "07:00",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
