package com.example.cunipro.ui.animals

import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.data.repository.AnimalRepository
import kotlinx.coroutines.launch

class AnimalsViewModel(private val repository: AnimalRepository) : ViewModel() {

    val activeAnimals: LiveData<List<AnimalEntity>> = repository.activeAnimals.asLiveData()
    val allAnimals: LiveData<List<AnimalEntity>> = repository.allAnimals.asLiveData()

    fun getAnimal(id: Long): LiveData<AnimalEntity?> = repository.getAnimalFlowById(id).asLiveData()

    fun getSuggestedTagNumber(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val suggested = repository.generateSuggestedTagNumber()
            onResult(suggested)
        }
    }

    fun addAnimal(
        tagNumber: String,
        sex: String,
        breed: String?,
        birthDate: String?,
        weightGrams: Int?,
        origin: String?,
        category: String,
        notes: String?,
        status: String? = null
    ) {
        viewModelScope.launch {
            val animalStatus = status ?: when (category.uppercase()) {
                "DECEDE", "DÉCÉDÉ" -> "DECEDE"
                "VENDU" -> "VENDU"
                else -> "VIVANT"
            }
            val animal = AnimalEntity(
                tagNumber = tagNumber,
                sex = sex,
                category = category,
                status = animalStatus,
                breed = breed,
                birthDate = birthDate,
                weightGrams = weightGrams,
                origin = origin,
                notes = notes
            )
            repository.insertAnimal(animal)
        }
    }

    fun updateAnimal(animal: AnimalEntity) {
        viewModelScope.launch {
            repository.updateAnimal(animal)
        }
    }

    fun deleteAnimal(animal: AnimalEntity) {
        viewModelScope.launch {
            repository.deleteAnimal(animal)
        }
    }
}

class AnimalsViewModelFactory(private val repository: AnimalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnimalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnimalsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
