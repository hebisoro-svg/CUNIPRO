package com.example.cunipro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_logs")
data class FinanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "RECETTE" ou "DEPENSE"
    val category: String, // "VENTE_LAPIN", "VENTE_ALIMENT", "VENTE_MATERIEL", "ACHAT_ALIMENT", "SOINS", "AUTRE"
    val amount: Double,
    val transactionDate: String, // "YYYY-MM-DD"
    val description: String,
    val clientName: String? = null, // Nom du client (ventes) ou fournisseur (dépenses)
    val itemDetails: String? = null, // ex: "1 lapin (L-003)" ou "20 kg de granulés"
    val quantity: Int? = null,
    val animalId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

