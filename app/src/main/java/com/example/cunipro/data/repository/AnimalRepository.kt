package com.example.cunipro.data.repository

import com.example.cunipro.data.local.dao.AnimalDao
import com.example.cunipro.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

class AnimalRepository(private val animalDao: AnimalDao) {

    val allAnimals: Flow<List<AnimalEntity>> = animalDao.getAllAnimals()
    val activeAnimals: Flow<List<AnimalEntity>> = animalDao.getActiveAnimals()
    val activeAnimalCount: Flow<Int> = animalDao.getActiveAnimalCount()

    fun getAnimalsByCategory(category: String): Flow<List<AnimalEntity>> =
        animalDao.getAnimalsByCategory(category)

    fun getAnimalsBySex(sex: String): Flow<List<AnimalEntity>> =
        animalDao.getAnimalsBySex(sex)

    suspend fun getAnimalById(id: Long): AnimalEntity? =
        animalDao.getAnimalById(id)

    suspend fun getAnimalByTagNumber(tagNumber: String): AnimalEntity? =
        animalDao.getAnimalByTagNumber(tagNumber)

    fun getAnimalFlowById(id: Long): Flow<AnimalEntity?> =
        animalDao.getAnimalFlowById(id)

    suspend fun generateSuggestedTagNumber(): String {
        val maxId = animalDao.getMaxId() ?: 0L
        val nextNumber = maxId + 1
        return String.format(java.util.Locale.US, "L-%03d", nextNumber)
    }

    suspend fun insertAnimal(animal: AnimalEntity): Long =
        animalDao.insertAnimal(animal)

    suspend fun updateAnimal(animal: AnimalEntity) =
        animalDao.updateAnimal(animal)

    suspend fun deleteAnimal(animal: AnimalEntity) =
        animalDao.deleteAnimal(animal)

    fun getActiveCountByCategory(category: String): Flow<Int> =
        animalDao.getActiveCountByCategory(category)

    val deceasedAnimalCount: Flow<Int> = animalDao.getDeceasedAnimalCount()
    val soldAnimalCount: Flow<Int> = animalDao.getSoldAnimalCount()
    val totalAnimalCount: Flow<Int> = animalDao.getTotalAnimalCount()

    fun getActiveCountBySex(sex: String): Flow<Int> =
        animalDao.getActiveCountBySex(sex)
}
