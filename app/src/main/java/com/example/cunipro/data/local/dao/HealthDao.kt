package com.example.cunipro.data.local.dao

import androidx.room.*
import com.example.cunipro.data.local.entity.HealthLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_logs WHERE isMortality = 0 ORDER BY createdAt DESC")
    fun getAllTreatments(): Flow<List<HealthLogEntity>>

    @Query("SELECT * FROM health_logs WHERE isMortality = 1 ORDER BY createdAt DESC")
    fun getMortalityLogs(): Flow<List<HealthLogEntity>>

    @Query("SELECT * FROM health_logs ORDER BY createdAt DESC")
    fun getAllHealthLogs(): Flow<List<HealthLogEntity>>

    @Query("SELECT COUNT(*) FROM health_logs WHERE isMortality = 0 AND status = 'EN_COURS'")
    fun getActiveTreatmentCount(): Flow<Int>

    @Query("SELECT * FROM health_logs WHERE isMortality = 0 AND status = 'EN_COURS'")
    suspend fun getActiveTreatmentsList(): List<HealthLogEntity>

    @Query("SELECT * FROM health_logs WHERE isMortality = 0 AND status = 'EN_COURS' AND nextDueDate IS NOT NULL AND nextDueDate != '' ORDER BY nextDueDate ASC LIMIT 1")
    fun getUpcomingReminder(): Flow<HealthLogEntity?>

    @Query("SELECT * FROM health_logs")
    suspend fun getAllHealthLogsList(): List<HealthLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLog(healthLog: HealthLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLogs(healthLogs: List<HealthLogEntity>)

    @Update
    suspend fun updateHealthLog(healthLog: HealthLogEntity)

    @Delete
    suspend fun deleteHealthLog(healthLog: HealthLogEntity)
}
