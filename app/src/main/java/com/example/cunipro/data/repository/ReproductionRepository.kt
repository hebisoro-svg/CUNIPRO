package com.example.cunipro.data.repository

import android.content.Context
import com.example.cunipro.data.local.dao.ReproductionDao
import com.example.cunipro.data.local.dao.TaskDao
import com.example.cunipro.data.local.entity.ReproductionEntity
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.util.BreedingDateCalculator
import kotlinx.coroutines.flow.Flow

class ReproductionRepository(
    private val context: Context,
    private val reproductionDao: ReproductionDao,
    private val taskDao: TaskDao
) {

    val allReproductions: Flow<List<ReproductionEntity>> = reproductionDao.getAllReproductions()
    val activeReproductions: Flow<List<ReproductionEntity>> = reproductionDao.getActiveReproductions()
    val activeReproductionCount: Flow<Int> = reproductionDao.getActiveReproductionCount()
    val totalKitsBornAlive: Flow<Int?> = reproductionDao.getTotalKitsBornAlive()
    val totalKitsBornDead: Flow<Int?> = reproductionDao.getTotalKitsBornDead()
    val totalWeanedKitsCount: Flow<Int?> = reproductionDao.getTotalWeanedKitsCount()
    val totalMatingCount: Flow<Int> = reproductionDao.getTotalMatingCount()
    val successfulMatingCount: Flow<Int> = reproductionDao.getSuccessfulMatingCount()

    suspend fun getReproductionById(id: Long): ReproductionEntity? =
        reproductionDao.getReproductionById(id)

    suspend fun insertMating(
        femaleId: Long,
        femaleTag: String,
        maleId: Long?,
        maleTag: String?,
        matingDate: String,
        notes: String? = null
    ): Long {
        val palpationDays = BreedingDateCalculator.getPalpationDays(context)
        val gestationDays = BreedingDateCalculator.getGestationDays(context)

        val palpationDate = BreedingDateCalculator.calculatePalpationDate(matingDate, palpationDays)
        val expectedKindlingDate = BreedingDateCalculator.calculateExpectedKindlingDate(matingDate, gestationDays)

        val reproduction = ReproductionEntity(
            femaleId = femaleId,
            femaleTag = femaleTag,
            maleId = maleId,
            maleTag = maleTag,
            matingDate = matingDate,
            palpationDate = palpationDate,
            expectedKindlingDate = expectedKindlingDate,
            status = "EN_COURS",
            notes = notes
        )

        val repoId = reproductionDao.insertReproduction(reproduction)

        // Automatically create notification tasks for Palpation & Kindling
        taskDao.insertTask(
            TaskEntity(
                title = "Palpation lapine $femaleTag",
                dueDate = palpationDate,
                priority = "HAUTE",
                type = "PALPATION",
                relatedEntityId = repoId
            )
        )

        taskDao.insertTask(
            TaskEntity(
                title = "Mise bas prévue lapine $femaleTag",
                dueDate = expectedKindlingDate,
                priority = "HAUTE",
                type = "MISE_BAS",
                relatedEntityId = repoId
            )
        )

        return repoId
    }

    suspend fun updatePalpationResult(
        reproduction: ReproductionEntity,
        isPregnant: Boolean
    ) {
        val updated = if (isPregnant) {
            reproduction.copy(palpationResult = true)
        } else {
            reproduction.copy(palpationResult = false, status = "ECHOUE")
        }
        reproductionDao.updateReproduction(updated)
    }

    suspend fun recordKindling(
        reproduction: ReproductionEntity,
        actualKindlingDate: String,
        bornAlive: Int,
        bornDead: Int
    ) {
        val sevrageDays = BreedingDateCalculator.getSevrageDays(context)
        val expectedWeaningDate = BreedingDateCalculator.calculateExpectedWeaningDate(actualKindlingDate, sevrageDays)
        val updated = reproduction.copy(
            actualKindlingDate = actualKindlingDate,
            bornAliveCount = bornAlive,
            bornDeadCount = bornDead,
            expectedWeaningDate = expectedWeaningDate
        )
        reproductionDao.updateReproduction(updated)

        // Automatically create notification task for Weaning (Sevrage)
        taskDao.insertTask(
            TaskEntity(
                title = "Sevrage portée lapine ${reproduction.femaleTag} ($bornAlive vivants)",
                dueDate = expectedWeaningDate,
                priority = "HAUTE",
                type = "SEVRAGE",
                relatedEntityId = reproduction.id
            )
        )
    }

    suspend fun recordWeaning(
        reproduction: ReproductionEntity,
        actualWeaningDate: String,
        weanedCount: Int
    ) {
        val updated = reproduction.copy(
            actualWeaningDate = actualWeaningDate,
            weanedCount = weanedCount,
            status = "TERMINE"
        )
        reproductionDao.updateReproduction(updated)
    }

    suspend fun deleteReproduction(reproduction: ReproductionEntity) =
        reproductionDao.deleteReproduction(reproduction)
}
