package com.example.cunipro.data.local.dao

import androidx.room.*
import com.example.cunipro.data.local.entity.FinanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_logs ORDER BY transactionDate DESC, id DESC")
    fun getAllFinanceLogs(): Flow<List<FinanceLogEntity>>

    @Query("SELECT * FROM finance_logs WHERE type = 'RECETTE' ORDER BY transactionDate DESC, id DESC")
    fun getIncomeLogs(): Flow<List<FinanceLogEntity>>

    @Query("SELECT * FROM finance_logs WHERE type = 'DEPENSE' ORDER BY transactionDate DESC, id DESC")
    fun getExpenseLogs(): Flow<List<FinanceLogEntity>>

    @Query("SELECT * FROM finance_logs")
    suspend fun getAllFinanceLogsList(): List<FinanceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinanceLog(log: FinanceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinanceLogs(logs: List<FinanceLogEntity>)

    @Delete
    suspend fun deleteFinanceLog(log: FinanceLogEntity)

    @Query("SELECT SUM(amount) FROM finance_logs WHERE type = 'RECETTE'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM finance_logs WHERE type = 'DEPENSE'")
    fun getTotalExpenses(): Flow<Double?>
}

