package com.example.cunipro.ui.reproduction

import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.ReproductionEntity
import com.example.cunipro.data.repository.ReproductionRepository
import kotlinx.coroutines.launch

class ReproductionViewModel(private val repository: ReproductionRepository) : ViewModel() {

    val activeReproductions: LiveData<List<ReproductionEntity>> = repository.activeReproductions.asLiveData()
    val allReproductions: LiveData<List<ReproductionEntity>> = repository.allReproductions.asLiveData()
    val activeReproductionCount: LiveData<Int> = repository.activeReproductionCount.asLiveData()
    val totalKitsBornAlive: LiveData<Int?> = repository.totalKitsBornAlive.asLiveData()
    val totalMatingCount: LiveData<Int> = repository.totalMatingCount.asLiveData()
    val successfulMatingCount: LiveData<Int> = repository.successfulMatingCount.asLiveData()

    val successRate: LiveData<Double> = MediatorLiveData<Double>().apply {
        var total = 0
        var success = 0

        fun update() {
            value = if (total > 0) (success.toDouble() / total.toDouble()) * 100.0 else 0.0
        }

        addSource(totalMatingCount) { t ->
            total = t ?: 0
            update()
        }
        addSource(successfulMatingCount) { s ->
            success = s ?: 0
            update()
        }
    }

    fun recordMating(
        femaleTag: String,
        maleTag: String?,
        matingDate: String,
        femaleId: Long = 0L,
        maleId: Long? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.insertMating(
                femaleId = femaleId,
                femaleTag = femaleTag,
                maleId = maleId,
                maleTag = maleTag.ifBlank { null },
                matingDate = matingDate,
                notes = notes
            )
        }
    }

    fun recordPalpation(reproduction: ReproductionEntity, isPregnant: Boolean) {
        viewModelScope.launch {
            repository.updatePalpationResult(reproduction, isPregnant)
        }
    }

    fun recordKindling(
        reproduction: ReproductionEntity,
        kindlingDate: String,
        bornAlive: Int,
        bornDead: Int
    ) {
        viewModelScope.launch {
            repository.recordKindling(reproduction, kindlingDate, bornAlive, bornDead)
        }
    }

    fun recordWeaning(
        reproduction: ReproductionEntity,
        weaningDate: String,
        weanedCount: Int
    ) {
        viewModelScope.launch {
            repository.recordWeaning(reproduction, weaningDate, weanedCount)
        }
    }
}

private fun String?.ifBlank(defaultValue: () -> String?): String? =
    if (this == null || this.trim().isEmpty()) defaultValue() else this

class ReproductionViewModelFactory(private val repository: ReproductionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReproductionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReproductionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
