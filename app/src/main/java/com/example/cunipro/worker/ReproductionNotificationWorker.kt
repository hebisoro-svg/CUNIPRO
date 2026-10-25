package com.example.cunipro.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cunipro.CuniProApplication
import com.example.cunipro.data.local.entity.NotificationEntity
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.NotificationHelper

class ReproductionNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? CuniProApplication ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("cuni_alert_settings", Context.MODE_PRIVATE)

        val isMiseBasEnabled = prefs.getBoolean("mise_bas", true)
        val isSevrageEnabled = prefs.getBoolean("sevrage", true)
        val isStockFaibleEnabled = prefs.getBoolean("stock_faible", true)
        val isSoinsEnabled = prefs.getBoolean("soins", true)
        val morningTime = prefs.getString("morning_time", "07:00") ?: "07:00"

        val todayStr = BreedingDateCalculator.getTodayString()
        val tomorrowStr = BreedingDateCalculator.addDaysToDate(todayStr, 1)

        val db = application.database

        val isFr = LanguageManager.isFrench(applicationContext)

        // --- 1. REPRODUCTION ALERTS (Mise bas, Sevrage, Palpation) ---
        val activeReproductions = db.reproductionDao().getActiveReproductionsList()
        for (repo in activeReproductions) {
            // Palpation
            if (repo.palpationDate == todayStr && repo.palpationResult == null) {
                val title = if (isFr) "Palpation à effectuer 🐇" else "Palpation required 🐇"
                val message = if (isFr) "Palpation prévue aujourd'hui pour la lapine ${repo.femaleTag}." else "Palpation scheduled today for female rabbit ${repo.femaleTag}."
                sendAndSaveNotification(application, repo.id.toInt() * 10 + 1, title, message, "PALPATION", todayStr, morningTime)
            }

            // Mise Bas
            if (isMiseBasEnabled && repo.actualKindlingDate == null && repo.expectedKindlingDate != null) {
                if (repo.expectedKindlingDate == todayStr) {
                    val title = if (isFr) "Mise bas prévue aujourd'hui 💖" else "Kindling expected today 💖"
                    val message = if (isFr) "Mise bas attendue aujourd'hui pour la lapine ${repo.femaleTag} !" else "Kindling expected today for female rabbit ${repo.femaleTag}!"
                    sendAndSaveNotification(application, repo.id.toInt() * 10 + 2, title, message, "MISE_BAS", todayStr, morningTime)
                } else if (repo.expectedKindlingDate == tomorrowStr) {
                    val title = if (isFr) "Mise bas prévisionnelle demain 📦" else "Kindling expected tomorrow 📦"
                    val message = if (isFr) "Préparez le nid et surveillez la lapine ${repo.femaleTag}." else "Prepare the nest box and check female rabbit ${repo.femaleTag}."
                    sendAndSaveNotification(application, repo.id.toInt() * 10 + 3, title, message, "MISE_BAS", todayStr, morningTime)
                }
            }

            // Sevrage
            if (isSevrageEnabled && repo.actualKindlingDate != null && repo.actualWeaningDate == null && repo.expectedWeaningDate != null) {
                if (repo.expectedWeaningDate == todayStr) {
                    val title = if (isFr) "Sevrage à effectuer aujourd'hui 🌾" else "Weaning due today 🌾"
                    val message = if (isFr) "Sevrage prévu aujourd'hui pour la portée de la lapine ${repo.femaleTag}." else "Weaning scheduled today for the litter of female rabbit ${repo.femaleTag}."
                    sendAndSaveNotification(application, repo.id.toInt() * 10 + 4, title, message, "SEVRAGE", todayStr, morningTime)
                }
            }
        }

        // --- 2. LOW FEED STOCK CHECK ---
        if (isStockFaibleEnabled) {
            val stockItems = db.feedDao().getAllStockList()
            for (stock in stockItems) {
                if (stock.quantityKg <= stock.minThresholdKg) {
                    val title = if (isFr) "Stock faible : ${stock.feedName} ⚠️" else "Low feed stock: ${stock.feedName} ⚠️"
                    val message = if (isFr) "Il reste ${stock.quantityKg} kg (seuil : ${stock.minThresholdKg} kg)." else "Only ${stock.quantityKg} kg remaining (threshold: ${stock.minThresholdKg} kg)."
                    sendAndSaveNotification(application, 2000 + stock.id.toInt(), title, message, "STOCK", todayStr, morningTime)
                }
            }
        }

        // --- 3. HEALTH CARE ALERTS (Soins) ---
        if (isSoinsEnabled) {
            val treatments = db.healthDao().getActiveTreatmentsList()
            for (log in treatments) {
                val dueDate = if (!log.nextDueDate.isNullOrBlank()) log.nextDueDate else log.treatmentDate
                if (!dueDate.isNullOrBlank()) {
                    if (dueDate < todayStr) {
                        // Date is in the past: auto-complete care status
                        db.healthDao().updateHealthLog(log.copy(status = "TERMINE"))
                    } else if (dueDate == todayStr) {
                        val title = if (isFr) "Soin à effectuer : ${log.treatmentType} 🩺" else "Care required: ${log.treatmentType} 🩺"
                        val message = if (isFr) "${log.treatmentType} prévu pour ${log.animalTag ?: "un animal"} ($dueDate)." else "${log.treatmentType} planned for ${log.animalTag ?: "an animal"} ($dueDate)."
                        sendAndSaveNotification(application, 3000 + log.id.toInt(), title, message, "SOIN", todayStr, morningTime)
                        // After sending notification for today, update status to TERMINE so it won't repeat tomorrow
                        db.healthDao().updateHealthLog(log.copy(status = "TERMINE"))
                    }
                }
            }
        }

        return Result.success()
    }

    private suspend fun sendAndSaveNotification(
        app: CuniProApplication,
        notificationId: Int,
        title: String,
        message: String,
        type: String,
        date: String,
        time: String
    ) {
        // Envoi système téléphone
        NotificationHelper.sendNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = message
        )

        // Sauvegarde base locale Room
        app.database.notificationDao().insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = type,
                date = date,
                time = time,
                isRead = false
            )
        )
    }
}
