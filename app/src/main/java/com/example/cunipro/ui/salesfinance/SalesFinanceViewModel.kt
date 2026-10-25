package com.example.cunipro.ui.salesfinance

import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.data.local.entity.FinanceLogEntity
import com.example.cunipro.data.repository.AnimalRepository
import com.example.cunipro.data.repository.FinanceRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class ExpenseBreakdown(
    val feedAmount: Double = 0.0,
    val healthAmount: Double = 0.0,
    val otherAmount: Double = 0.0,
    val totalExpenses: Double = 0.0
)

class SalesFinanceViewModel(
    private val financeRepository: FinanceRepository,
    private val animalRepository: AnimalRepository
) : ViewModel() {

    val totalIncome: LiveData<Double?> = financeRepository.totalIncome.asLiveData()
    val totalExpenses: LiveData<Double?> = financeRepository.totalExpenses.asLiveData()

    val netBalance: LiveData<Double> = MediatorLiveData<Double>().apply {
        var inc = 0.0
        var exp = 0.0

        fun update() {
            value = inc - exp
        }

        addSource(totalIncome) { i ->
            inc = i ?: 0.0
            update()
        }
        addSource(totalExpenses) { e ->
            exp = e ?: 0.0
            update()
        }
    }

    val salesLogs: LiveData<List<FinanceLogEntity>> = financeRepository.incomeLogs.asLiveData()
    val expenseLogs: LiveData<List<FinanceLogEntity>> = financeRepository.expenseLogs.asLiveData()

    val expenseBreakdown: LiveData<ExpenseBreakdown> = expenseLogs.map { logs ->
        var feed = 0.0
        var health = 0.0
        var other = 0.0

        logs.forEach { log ->
            val cat = log.category.uppercase(Locale.ROOT)
            when {
                cat.contains("ALIMENT") -> feed += log.amount
                cat.contains("SOIN") || cat.contains("SANTE") || cat.contains("SANTÉ") -> health += log.amount
                else -> other += log.amount
            }
        }
        val total = feed + health + other
        ExpenseBreakdown(feed, health, other, total)
    }

    // List of living animals that are NOT deceased and NOT already sold
    val availableAnimals: LiveData<List<AnimalEntity>> = animalRepository.activeAnimals.map { list ->
        list.filter { animal ->
            !animal.status.equals("DECEDE", ignoreCase = true) &&
            !animal.status.equals("DÉCÉDÉ", ignoreCase = true) &&
            !animal.status.equals("VENDU", ignoreCase = true) &&
            !animal.category.contains("DECED", ignoreCase = true) &&
            !animal.category.equals("VENDU", ignoreCase = true)
        }
    }.asLiveData()

    fun recordSale(
        category: String,
        amount: Double,
        date: String,
        description: String,
        clientName: String?,
        itemDetails: String?,
        quantity: Int?,
        selectedAnimalId: Long?,
        selectedAnimalTag: String? = null
    ) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val log = FinanceLogEntity(
                    type = "RECETTE",
                    category = category,
                    amount = amount,
                    transactionDate = date,
                    description = description,
                    clientName = clientName,
                    itemDetails = itemDetails,
                    quantity = quantity,
                    animalId = selectedAnimalId
                )
                financeRepository.insertFinanceLog(log)

                // If a specific rabbit was selected or identified by tag, update status & category to VENDU in livestock
                var animalToUpdate: AnimalEntity? = null
                if (selectedAnimalId != null && selectedAnimalId > 0) {
                    animalToUpdate = animalRepository.getAnimalById(selectedAnimalId)
                }
                if (animalToUpdate == null && !selectedAnimalTag.isNullOrBlank()) {
                    animalToUpdate = animalRepository.getAnimalByTagNumber(selectedAnimalTag)
                }

                if (animalToUpdate != null) {
                    val updated = animalToUpdate.copy(
                        status = "VENDU",
                        category = "VENDU"
                    )
                    animalRepository.updateAnimal(updated)
                }
            }
        }
    }

    fun recordExpense(
        category: String,
        amount: Double,
        date: String,
        description: String,
        supplierOrNote: String?
    ) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val log = FinanceLogEntity(
                    type = "DEPENSE",
                    category = category,
                    amount = amount,
                    transactionDate = date,
                    description = description,
                    clientName = supplierOrNote,
                    itemDetails = null,
                    quantity = null
                )
                financeRepository.insertFinanceLog(log)
            }
        }
    }
}

class SalesFinanceViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val animalRepository: AnimalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesFinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesFinanceViewModel(financeRepository, animalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
