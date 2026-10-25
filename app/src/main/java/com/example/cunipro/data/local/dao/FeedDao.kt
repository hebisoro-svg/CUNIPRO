package com.example.cunipro.data.local.dao

import androidx.room.*
import com.example.cunipro.data.local.entity.FeedDistributionEntity
import com.example.cunipro.data.local.entity.FeedStockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_stock ORDER BY feedName ASC")
    fun getAllStock(): Flow<List<FeedStockEntity>>

    @Query("SELECT * FROM feed_stock")
    suspend fun getAllStockList(): List<FeedStockEntity>

    @Query("SELECT * FROM feed_stock WHERE id = :id LIMIT 1")
    suspend fun getStockById(id: Long): FeedStockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: FeedStockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<FeedStockEntity>)

    @Update
    suspend fun updateStock(stock: FeedStockEntity)

    @Delete
    suspend fun deleteStock(stock: FeedStockEntity)

    @Query("SELECT * FROM feed_distribution ORDER BY distributionDate DESC")
    fun getAllDistributions(): Flow<List<FeedDistributionEntity>>

    @Query("SELECT * FROM feed_distribution")
    suspend fun getAllDistributionsList(): List<FeedDistributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistribution(distribution: FeedDistributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistributions(distributions: List<FeedDistributionEntity>)

    @Query("SELECT * FROM feed_stock WHERE LOWER(feedName) = LOWER(:name) LIMIT 1")
    suspend fun getStockByName(name: String): FeedStockEntity?

    @Query("SELECT * FROM feed_stock WHERE quantityKg <= minThresholdKg ORDER BY feedName ASC")
    fun getLowStockItems(): Flow<List<FeedStockEntity>>

    @Query("SELECT SUM(quantityKg) FROM feed_stock")
    fun getTotalStockKg(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM feed_stock WHERE quantityKg <= minThresholdKg")
    fun getLowStockAlertCount(): Flow<Int>
}
