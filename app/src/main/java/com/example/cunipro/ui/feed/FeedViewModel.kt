package com.example.cunipro.ui.feed

import androidx.lifecycle.*
import com.example.cunipro.data.local.entity.FeedDistributionEntity
import com.example.cunipro.data.local.entity.FeedStockEntity
import com.example.cunipro.data.repository.FeedRepository
import com.example.cunipro.util.BreedingDateCalculator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    val feedStockList: LiveData<List<FeedStockEntity>> = repository.allStock.asLiveData()
    val allDistributions: LiveData<List<FeedDistributionEntity>> = repository.allDistributions.asLiveData()

    fun addOrUpdateFeedStock(name: String, quantityKg: Double, minThresholdKg: Double) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.addOrUpdateFeedStock(name, quantityKg, minThresholdKg)
            }
        }
    }

    fun recordConsumption(feedName: String, qtyKg: Double, distributionDate: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.recordConsumption(feedName, qtyKg, distributionDate)
            }
        }
    }

    fun distributeFeed(stock: FeedStockEntity, qtyKg: Double) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val distribution = FeedDistributionEntity(
                    feedStockId = stock.id,
                    feedName = stock.feedName,
                    distributionDate = BreedingDateCalculator.getTodayString(),
                    quantityKg = qtyKg
                )
                repository.distributeFeed(distribution)
            }
        }
    }
}

class FeedViewModelFactory(private val repository: FeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
