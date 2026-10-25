package com.example.cunipro.data.repository

import com.example.cunipro.data.local.dao.FinanceDao
import com.example.cunipro.data.local.entity.FinanceLogEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {

    val allFinanceLogs: Flow<List<FinanceLogEntity>> = financeDao.getAllFinanceLogs()
    val incomeLogs: Flow<List<FinanceLogEntity>> = financeDao.getIncomeLogs()
    val expenseLogs: Flow<List<FinanceLogEntity>> = financeDao.getExpenseLogs()
    val totalIncome: Flow<Double?> = financeDao.getTotalIncome()
    val totalExpenses: Flow<Double?> = financeDao.getTotalExpenses()

    suspend fun insertFinanceLog(log: FinanceLogEntity): Long = financeDao.insertFinanceLog(log)
    suspend fun deleteFinanceLog(log: FinanceLogEntity) = financeDao.deleteFinanceLog(log)
}

