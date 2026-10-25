package com.example.cunipro.data.local.dao

import androidx.room.*
import com.example.cunipro.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animals ORDER BY tagNumber ASC")
    fun getAllAnimals(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE status = 'VIVANT' AND category NOT IN ('DECEDE', 'DÉCÉDÉ', 'VENDU') ORDER BY tagNumber ASC")
    fun getActiveAnimals(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE category = :category AND status = 'VIVANT' ORDER BY tagNumber ASC")
    fun getAnimalsByCategory(category: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE sex = :sex AND status = 'VIVANT' ORDER BY tagNumber ASC")
    fun getAnimalsBySex(sex: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE id = :id LIMIT 1")
    suspend fun getAnimalById(id: Long): AnimalEntity?

    @Query("SELECT * FROM animals WHERE tagNumber = :tagNumber LIMIT 1")
    suspend fun getAnimalByTagNumber(tagNumber: String): AnimalEntity?

    @Query("SELECT * FROM animals WHERE id = :id LIMIT 1")
    fun getAnimalFlowById(id: Long): Flow<AnimalEntity?>

    @Query("SELECT MAX(id) FROM animals")
    suspend fun getMaxId(): Long?

    @Query("SELECT * FROM animals")
    suspend fun getAllAnimalsList(): List<AnimalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimals(animals: List<AnimalEntity>)

    @Update
    suspend fun updateAnimal(animal: AnimalEntity)

    @Delete
    suspend fun deleteAnimal(animal: AnimalEntity)

    @Query("SELECT COUNT(*) FROM animals WHERE status = 'VIVANT'")
    fun getActiveAnimalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM animals WHERE (category = :category OR (:category = 'REPRODUCTEUR' AND category IN ('PRIMIPARE', 'MULTIPARE'))) AND status = 'VIVANT'")
    fun getActiveCountByCategory(category: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM animals WHERE status = 'DECEDE' OR category = 'DECEDE'")
    fun getDeceasedAnimalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM animals WHERE status = 'VENDU' OR category = 'VENDU'")
    fun getSoldAnimalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM animals")
    fun getTotalAnimalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM animals WHERE sex = :sex AND status = 'VIVANT'")
    fun getActiveCountBySex(sex: String): Flow<Int>
}
