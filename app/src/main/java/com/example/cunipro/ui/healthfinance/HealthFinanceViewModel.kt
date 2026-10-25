package com.example.cunipro.ui.healthfinance

import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.data.local.entity.FinanceLogEntity
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.data.repository.AnimalRepository
import com.example.cunipro.data.repository.FinanceRepository
import com.example.cunipro.data.repository.HealthRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthFinanceViewModel(
    private val healthRepository: HealthRepository,
    private val financeRepository: FinanceRepository,
    private val animalRepository: AnimalRepository
) : ViewModel() {

    val healthLogs: LiveData<List<HealthLogEntity>> = healthRepository.allHealthLogs.asLiveData()
    val treatments: LiveData<List<HealthLogEntity>> = healthRepository.allTreatments.asLiveData()
    val mortalityLogs: LiveData<List<HealthLogEntity>> = healthRepository.mortalityLogs.asLiveData()
    val activeTreatmentCount: LiveData<Int> = healthRepository.activeTreatmentCount.asLiveData()
    val upcomingReminder: LiveData<HealthLogEntity?> = healthRepository.upcomingReminder.asLiveData()

    val allAnimals: LiveData<List<AnimalEntity>> = animalRepository.allAnimals.asLiveData()

    val totalIncome: LiveData<Double?> = financeRepository.totalIncome.asLiveData()
    val totalExpenses: LiveData<Double?> = financeRepository.totalExpenses.asLiveData()

    init {
        checkExpiredCareLogs()
    }

    fun checkExpiredCareLogs() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                healthRepository.checkAndUpdateExpiredCareLogs()
            }
        }
    }

    fun toggleCareStatus(log: HealthLogEntity) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val newStatus = if (log.status.equals("EN_COURS", ignoreCase = true)) "TERMINE" else "EN_COURS"
                healthRepository.updateHealthLog(log.copy(status = newStatus))
            }
        }
    }

    fun addHealthLog(log: HealthLogEntity) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                healthRepository.addHealthLog(log)

                // Si le soin a un coût supérieur à 0, l'enregistrer dans les finances comme dépense SOINS
                if (log.cost > 0) {
                    val label = if (log.isMortality) "Frais sanitaire (Décès)" else "Soin/Traitement: ${log.treatmentName}"
                    financeRepository.insertFinanceLog(
                        FinanceLogEntity(
                            type = "DEPENSE",
                            category = "SOINS",
                            amount = log.cost,
                            description = "$label (${log.animalTag ?: "Élevage"})",
                            transactionDate = log.treatmentDate,
                            animalId = log.animalId
                        )
                    )
                }
            }
        }
    }
}

class HealthFinanceViewModelFactory(
    private val healthRepository: HealthRepository,
    private val financeRepository: FinanceRepository,
    private val animalRepository: AnimalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthFinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthFinanceViewModel(healthRepository, financeRepository, animalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
