package com.example.cunipro.ui.dashboard

import androidx.lifecycle.*
import com.example.cunipro.data.repository.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    animalRepository: AnimalRepository,
    reproductionRepository: ReproductionRepository,
    feedRepository: FeedRepository,
    financeRepository: FinanceRepository,
    taskRepository: TaskRepository,
    notificationRepository: NotificationRepository
) : ViewModel() {

    // 1. Cheptel (Rabbits)
    val activeAnimalCount = animalRepository.activeAnimalCount.asLiveData()
    val reproductionCategoryCount = animalRepository.getActiveCountByCategory("REPRODUCTEUR").asLiveData()
    val fatteningCategoryCount = animalRepository.getActiveCountByCategory("ENGRAISSEMENT").asLiveData()
    val weanedCategoryCount = animalRepository.getActiveCountByCategory("SEVRE").asLiveData()
    val primipareCategoryCount = animalRepository.getActiveCountByCategory("PRIMIPARE").asLiveData()
    val multipareCategoryCount = animalRepository.getActiveCountByCategory("MULTIPARE").asLiveData()
    val soldAnimalCount = animalRepository.soldAnimalCount.asLiveData()
    val maleCount = animalRepository.getActiveCountBySex("M").asLiveData()
    val femaleCount = animalRepository.getActiveCountBySex("F").asLiveData()

    // 2. Mortality
    val deceasedAnimalCount = animalRepository.deceasedAnimalCount.asLiveData()
    val totalAnimalCount = animalRepository.totalAnimalCount.asLiveData()
    val mortalityRate: LiveData<Double> = MediatorLiveData<Double>().apply {
        var deceased = 0
        var total = 0

        fun update() {
            value = if (total > 0) {
                (deceased.toDouble() / total.toDouble()) * 100.0
            } else {
                0.0
            }
        }

        addSource(deceasedAnimalCount) { d ->
            deceased = d ?: 0
            update()
        }
        addSource(totalAnimalCount) { t ->
            total = t ?: 0
            update()
        }
    }

    // 3. Finance & Revenue
    val totalIncome = financeRepository.totalIncome.asLiveData()
    val totalExpenses = financeRepository.totalExpenses.asLiveData()
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

    // 4. Feed Management
    val totalFeedStockKg = feedRepository.totalStockKg.asLiveData()
    val lowStockAlertCount = feedRepository.lowStockAlertCount.asLiveData()

    // 5. Reproduction Statistics
    val activeReproductionCount = reproductionRepository.activeReproductionCount.asLiveData()
    val totalKitsBornAlive = reproductionRepository.totalKitsBornAlive.asLiveData()
    val totalKitsBornDead = reproductionRepository.totalKitsBornDead.asLiveData()
    val totalKitsWeaned = reproductionRepository.totalWeanedKitsCount.asLiveData()

    // 6. Tasks & Notifications
    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val pendingTaskCount = taskRepository.pendingTaskCount.asLiveData()
    val unreadNotificationCount = notificationRepository.unreadCount.asLiveData()
    val todayTasks = taskRepository.getPendingTasksDueBy(todayString).asLiveData()
    val pendingTasks = taskRepository.pendingTasks.asLiveData()

    val totalNotificationBadgeCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        var tasks = 0
        var unread = 0

        fun update() {
            value = tasks + unread
        }

        addSource(pendingTaskCount) { t ->
            tasks = t ?: 0
            update()
        }
        addSource(unreadNotificationCount) { u ->
            unread = u ?: 0
            update()
        }
    }
}

class DashboardViewModelFactory(
    private val animalRepository: AnimalRepository,
    private val reproductionRepository: ReproductionRepository,
    private val feedRepository: FeedRepository,
    private val financeRepository: FinanceRepository,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                animalRepository,
                reproductionRepository,
                feedRepository,
                financeRepository,
                taskRepository,
                notificationRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
