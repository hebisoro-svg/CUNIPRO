package com.example.cunipro.data.local.dao

import androidx.room.*
import com.example.cunipro.data.local.entity.ReproductionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReproductionDao {
    @Query("SELECT * FROM reproductions ORDER BY matingDate DESC")
    fun getAllReproductions(): Flow<List<ReproductionEntity>>

    @Query("SELECT * FROM reproductions WHERE status = 'EN_COURS' ORDER BY matingDate DESC")
    fun getActiveReproductions(): Flow<List<ReproductionEntity>>

    @Query("SELECT * FROM reproductions WHERE status = 'EN_COURS'")
    suspend fun getActiveReproductionsList(): List<ReproductionEntity>

    @Query("SELECT * FROM reproductions WHERE id = :id LIMIT 1")
    suspend fun getReproductionById(id: Long): ReproductionEntity?

    @Query("SELECT * FROM reproductions")
    suspend fun getAllReproductionsList(): List<ReproductionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReproduction(reproduction: ReproductionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReproductions(reproductions: List<ReproductionEntity>)

    @Update
    suspend fun updateReproduction(reproduction: ReproductionEntity)

    @Delete
    suspend fun deleteReproduction(reproduction: ReproductionEntity)

    @Query("SELECT COUNT(*) FROM reproductions WHERE status = 'EN_COURS' AND actualKindlingDate IS NULL")
    fun getActiveReproductionCount(): Flow<Int>

    @Query("SELECT SUM(bornAliveCount) FROM reproductions")
    fun getTotalKitsBornAlive(): Flow<Int?>

    @Query("SELECT SUM(bornDeadCount) FROM reproductions")
    fun getTotalKitsBornDead(): Flow<Int?>

    @Query("SELECT SUM(weanedCount) FROM reproductions WHERE status = 'TERMINE' OR weanedCount > 0")
    fun getTotalWeanedKitsCount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM reproductions")
    fun getTotalMatingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reproductions WHERE (bornAliveCount > 0 OR status = 'TERMINE') AND status != 'ECHOUE'")
    fun getSuccessfulMatingCount(): Flow<Int>
}
