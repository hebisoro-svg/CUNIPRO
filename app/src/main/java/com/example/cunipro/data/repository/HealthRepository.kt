package com.example.cunipro.data.repository

import com.example.cunipro.data.local.dao.AnimalDao
import com.example.cunipro.data.local.dao.HealthDao
import com.example.cunipro.data.local.dao.TaskDao
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.util.BreedingDateCalculator
import kotlinx.coroutines.flow.Flow

class HealthRepository(
    private val healthDao: HealthDao,
    private val taskDao: TaskDao,
    private val animalDao: AnimalDao
) {

    val allTreatments: Flow<List<HealthLogEntity>> = healthDao.getAllTreatments()
    val mortalityLogs: Flow<List<HealthLogEntity>> = healthDao.getMortalityLogs()
    val allHealthLogs: Flow<List<HealthLogEntity>> = healthDao.getAllHealthLogs()
    val activeTreatmentCount: Flow<Int> = healthDao.getActiveTreatmentCount()
    val upcomingReminder: Flow<HealthLogEntity?> = healthDao.getUpcomingReminder()

    suspend fun checkAndUpdateExpiredCareLogs() {
        val activeTreatments = healthDao.getActiveTreatmentsList()
        val todayStr = BreedingDateCalculator.getTodayString()
        for (log in activeTreatments) {
            val dueDate = if (!log.nextDueDate.isNullOrBlank()) log.nextDueDate else log.treatmentDate
            if (!dueDate.isNullOrBlank() && dueDate < todayStr) {
                healthDao.updateHealthLog(log.copy(status = "TERMINE"))
            }
        }
    }

    suspend fun addHealthLog(log: HealthLogEntity): Long {
        val id = healthDao.insertHealthLog(log)

        // If it's a mortality declaration for a specific animal, update animal status to DECEDE
        if (log.isMortality) {
            val animalToUpdate = if (log.animalId != null && log.animalId > 0) {
                animalDao.getAnimalById(log.animalId)
            } else if (!log.animalTag.isNullOrBlank()) {
                val cleanTag = log.animalTag.split("·", "(").firstOrNull()?.trim() ?: log.animalTag.trim()
                animalDao.getAnimalByTagNumber(cleanTag)
            } else null

            if (animalToUpdate != null) {
                animalDao.updateAnimal(animalToUpdate.copy(status = "DECEDE", category = "DECEDE"))
            }
        }

        // If there's a next due date for booster/treatment, add a task
        if (!log.isMortality && !log.nextDueDate.isNullOrBlank()) {
            val titleText = if (log.diseaseName.isNotBlank()) log.diseaseName else log.treatmentName
            taskDao.insertTask(
                TaskEntity(
                    title = "Rappel soin: $titleText (${log.animalTag ?: "Élevage"})",
                    dueDate = log.nextDueDate,
                    priority = "HAUTE",
                    type = "VACCIN",
                    relatedEntityId = id
                )
            )
        }
        return id
    }

    suspend fun updateHealthLog(log: HealthLogEntity) = healthDao.updateHealthLog(log)

    suspend fun deleteHealthLog(log: HealthLogEntity) = healthDao.deleteHealthLog(log)
}
