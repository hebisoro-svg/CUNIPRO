package com.example.cunipro.data.repository

import android.content.Context
import com.example.cunipro.data.local.dao.FeedDao
import com.example.cunipro.data.local.dao.NotificationDao
import com.example.cunipro.data.local.entity.FeedDistributionEntity
import com.example.cunipro.data.local.entity.FeedStockEntity
import com.example.cunipro.data.local.entity.NotificationEntity
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class FeedRepository(
    private val context: Context,
    private val feedDao: FeedDao,
    private val notificationDao: NotificationDao
) {

    val allStock: Flow<List<FeedStockEntity>> = feedDao.getAllStock()
    val allDistributions: Flow<List<FeedDistributionEntity>> = feedDao.getAllDistributions()
    val totalStockKg: Flow<Double?> = feedDao.getTotalStockKg()
    val lowStockAlertCount: Flow<Int> = feedDao.getLowStockAlertCount()

    val lowStockItems: Flow<List<FeedStockEntity>> = feedDao.getLowStockItems()

    suspend fun insertStock(stock: FeedStockEntity): Long {
        val id = feedDao.insertStock(stock)
        val created = stock.copy(id = id)
        checkAndTriggerLowStockNotification(created)
        return id
    }

    suspend fun updateStock(stock: FeedStockEntity) {
        feedDao.updateStock(stock)
        checkAndTriggerLowStockNotification(stock)
    }

    suspend fun deleteStock(stock: FeedStockEntity) = feedDao.deleteStock(stock)

    private fun normalizeFeedName(name: String): String {
        return name.lowercase(Locale.FRANCE)
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .trim()
    }

    suspend fun getStockByName(name: String): FeedStockEntity? {
        val stocks = feedDao.getAllStockList()
        val target = normalizeFeedName(name)
        return stocks.find { normalizeFeedName(it.feedName) == target } ?: feedDao.getStockByName(name)
    }

    suspend fun addOrUpdateFeedStock(feedName: String, addQtyKg: Double, thresholdKg: Double) {
        val existing = getStockByName(feedName)
        if (existing != null) {
            val updated = existing.copy(
                quantityKg = existing.quantityKg + addQtyKg,
                minThresholdKg = thresholdKg,
                lastUpdated = System.currentTimeMillis()
            )
            updateStock(updated)
        } else {
            val newStock = FeedStockEntity(
                feedName = feedName,
                quantityKg = addQtyKg,
                minThresholdKg = thresholdKg
            )
            insertStock(newStock)
        }
    }

    suspend fun recordConsumption(feedName: String, qtyKg: Double, distributionDate: String) {
        val existing = getStockByName(feedName)

        if (existing != null) {
            val newQty = (existing.quantityKg - qtyKg).coerceAtLeast(0.0)
            val updated = existing.copy(quantityKg = newQty, lastUpdated = System.currentTimeMillis())
            updateStock(updated)

            val distribution = FeedDistributionEntity(
                feedStockId = existing.id,
                feedName = existing.feedName,
                distributionDate = distributionDate,
                quantityKg = qtyKg
            )
            feedDao.insertDistribution(distribution)
        } else {
            val newStock = FeedStockEntity(
                feedName = feedName,
                quantityKg = 0.0,
                minThresholdKg = 10.0
            )
            val insertedId = insertStock(newStock)

            val distribution = FeedDistributionEntity(
                feedStockId = insertedId,
                feedName = feedName,
                distributionDate = distributionDate,
                quantityKg = qtyKg
            )
            feedDao.insertDistribution(distribution)
        }
    }

    suspend fun distributeFeed(distribution: FeedDistributionEntity) {
        feedDao.insertDistribution(distribution)
        // Deduct from stock
        val currentStock = feedDao.getStockById(distribution.feedStockId)
        if (currentStock != null) {
            val newQty = (currentStock.quantityKg - distribution.quantityKg).coerceAtLeast(0.0)
            val updated = currentStock.copy(quantityKg = newQty, lastUpdated = System.currentTimeMillis())
            updateStock(updated)
        }
    }

    private suspend fun checkAndTriggerLowStockNotification(stock: FeedStockEntity) {
        val prefs = context.getSharedPreferences("cuni_alert_settings", Context.MODE_PRIVATE)
        val isStockFaibleEnabled = prefs.getBoolean("stock_faible", true)
        if (isStockFaibleEnabled && stock.quantityKg <= stock.minThresholdKg) {
            val todayStr = BreedingDateCalculator.getTodayString()
            val morningTime = prefs.getString("morning_time", "07:00") ?: "07:00"
            val title = "Stock faible : ${stock.feedName} ⚠️"
            val message = "Il reste ${stock.quantityKg} kg (seuil : ${stock.minThresholdKg} kg)."

            NotificationHelper.sendNotification(
                context = context,
                notificationId = 2000 + stock.id.toInt(),
                title = title,
                message = message
            )

            notificationDao.insertNotification(
                NotificationEntity(
                    title = title,
                    message = message,
                    type = "STOCK",
                    date = todayStr,
                    time = morningTime,
                    isRead = false
                )
            )
        }
    }
}
