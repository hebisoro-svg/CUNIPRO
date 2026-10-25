package com.example.cunipro.data.repository

import android.content.Context
import com.example.cunipro.data.local.CuniProDatabase
import com.example.cunipro.data.local.entity.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseSyncRepository(context: Context) {

    private val db = CuniProDatabase.getDatabase(context)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val prefs = context.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun getOrCreateDeviceId(): String {
        var deviceId = prefs.getString("pref_device_user_id", null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = "dev_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("pref_device_user_id", deviceId).apply()
        }
        return deviceId!!
    }

    suspend fun getOrEnsureUserId(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        return try {
            val authResult = awaitTask(auth.signInAnonymously())
            authResult.user?.uid ?: getOrCreateDeviceId()
        } catch (e: Exception) {
            getOrCreateDeviceId()
        }
    }

    suspend fun syncLocalToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = getOrEnsureUserId()

        try {
            val userDocRef = firestore.collection("users").document(uid)

            // 1. Animals
            val animals = db.animalDao().getAllAnimalsList()
            for (animal in animals) {
                awaitTask(
                    userDocRef.collection("animals")
                        .document(animal.id.toString())
                        .set(animalToMap(animal), SetOptions.merge())
                )
            }

            // 2. Reproductions
            val reproductions = db.reproductionDao().getAllReproductionsList()
            for (rep in reproductions) {
                awaitTask(
                    userDocRef.collection("reproductions")
                        .document(rep.id.toString())
                        .set(reproductionToMap(rep), SetOptions.merge())
                )
            }

            // 3. Health Logs
            val healthLogs = db.healthDao().getAllHealthLogsList()
            for (log in healthLogs) {
                awaitTask(
                    userDocRef.collection("health_logs")
                        .document(log.id.toString())
                        .set(healthLogToMap(log), SetOptions.merge())
                )
            }

            // 4. Feed Stocks
            val feedStocks = db.feedDao().getAllStockList()
            for (stock in feedStocks) {
                awaitTask(
                    userDocRef.collection("feed_stocks")
                        .document(stock.id.toString())
                        .set(feedStockToMap(stock), SetOptions.merge())
                )
            }

            // 5. Feed Distributions
            val feedDistributions = db.feedDao().getAllDistributionsList()
            for (dist in feedDistributions) {
                awaitTask(
                    userDocRef.collection("feed_distributions")
                        .document(dist.id.toString())
                        .set(feedDistributionToMap(dist), SetOptions.merge())
                )
            }

            // 6. Finances
            val finances = db.financeDao().getAllFinanceLogsList()
            for (fin in finances) {
                awaitTask(
                    userDocRef.collection("finance_logs")
                        .document(fin.id.toString())
                        .set(financeLogToMap(fin), SetOptions.merge())
                )
            }

            // 7. Tasks
            val tasks = db.taskDao().getAllTasksList()
            for (task in tasks) {
                awaitTask(
                    userDocRef.collection("tasks")
                        .document(task.id.toString())
                        .set(taskToMap(task), SetOptions.merge())
                )
            }

            // 8. Notifications
            val notifications = db.notificationDao().getAllNotificationsList()
            for (notif in notifications) {
                awaitTask(
                    userDocRef.collection("notifications")
                        .document(notif.id.toString())
                        .set(notificationToMap(notif), SetOptions.merge())
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncCloudToLocal(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = getOrEnsureUserId()

        try {
            val userDocRef = firestore.collection("users").document(uid)

            // 1. Animals
            val animalsSnap = awaitTask(userDocRef.collection("animals").get())
            val animalsList = animalsSnap.documents.mapNotNull { doc -> mapToAnimal(doc.data ?: emptyMap()) }
            if (animalsList.isNotEmpty()) {
                db.animalDao().insertAnimals(animalsList)
            }

            // 2. Reproductions
            val repSnap = awaitTask(userDocRef.collection("reproductions").get())
            val repList = repSnap.documents.mapNotNull { doc -> mapToReproduction(doc.data ?: emptyMap()) }
            if (repList.isNotEmpty()) {
                db.reproductionDao().insertReproductions(repList)
            }

            // 3. Health Logs
            val healthSnap = awaitTask(userDocRef.collection("health_logs").get())
            val healthList = healthSnap.documents.mapNotNull { doc -> mapToHealthLog(doc.data ?: emptyMap()) }
            if (healthList.isNotEmpty()) {
                db.healthDao().insertHealthLogs(healthList)
            }

            // 4. Feed Stocks
            val stockSnap = awaitTask(userDocRef.collection("feed_stocks").get())
            val stockList = stockSnap.documents.mapNotNull { doc -> mapToFeedStock(doc.data ?: emptyMap()) }
            if (stockList.isNotEmpty()) {
                db.feedDao().insertStocks(stockList)
            }

            // 5. Feed Distributions
            val distSnap = awaitTask(userDocRef.collection("feed_distributions").get())
            val distList = distSnap.documents.mapNotNull { doc -> mapToFeedDistribution(doc.data ?: emptyMap()) }
            if (distList.isNotEmpty()) {
                db.feedDao().insertDistributions(distList)
            }

            // 6. Finances
            val finSnap = awaitTask(userDocRef.collection("finance_logs").get())
            val finList = finSnap.documents.mapNotNull { doc -> mapToFinanceLog(doc.data ?: emptyMap()) }
            if (finList.isNotEmpty()) {
                db.financeDao().insertFinanceLogs(finList)
            }

            // 7. Tasks
            val taskSnap = awaitTask(userDocRef.collection("tasks").get())
            val taskList = taskSnap.documents.mapNotNull { doc -> mapToTask(doc.data ?: emptyMap()) }
            if (taskList.isNotEmpty()) {
                db.taskDao().insertTasks(taskList)
            }

            // 8. Notifications
            val notifSnap = awaitTask(userDocRef.collection("notifications").get())
            val notifList = notifSnap.documents.mapNotNull { doc -> mapToNotification(doc.data ?: emptyMap()) }
            if (notifList.isNotEmpty()) {
                db.notificationDao().insertAll(notifList)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fullSync(): Result<Unit> = withContext(Dispatchers.IO) {
        val pushResult = syncLocalToCloud()
        if (pushResult.isFailure) return@withContext pushResult
        return@withContext syncCloudToLocal()
    }

    suspend fun getPendingSyncCount(): Int = withContext(Dispatchers.IO) {
        val count = db.animalDao().getAllAnimalsList().size +
                db.reproductionDao().getAllReproductionsList().size +
                db.healthDao().getAllHealthLogsList().size +
                db.feedDao().getAllStockList().size +
                db.feedDao().getAllDistributionsList().size +
                db.financeDao().getAllFinanceLogsList().size +
                db.taskDao().getAllTasksList().size +
                db.notificationDao().getAllNotificationsList().size
        return@withContext count
    }

    // --- Mappers ---

    private fun animalToMap(a: AnimalEntity) = mapOf(
        "id" to a.id,
        "tagNumber" to a.tagNumber,
        "sex" to a.sex,
        "category" to a.category,
        "status" to a.status,
        "breed" to a.breed,
        "birthDate" to a.birthDate,
        "weightGrams" to a.weightGrams,
        "cageNumber" to a.cageNumber,
        "origin" to a.origin,
        "notes" to a.notes,
        "createdAt" to a.createdAt
    )

    private fun mapToAnimal(map: Map<String, Any>): AnimalEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return AnimalEntity(
            id = id,
            tagNumber = map["tagNumber"] as? String ?: "",
            sex = map["sex"] as? String ?: "F",
            category = map["category"] as? String ?: "ENGRAISSEMENT",
            status = map["status"] as? String ?: "VIVANT",
            breed = map["breed"] as? String,
            birthDate = map["birthDate"] as? String,
            weightGrams = (map["weightGrams"] as? Number)?.toInt(),
            cageNumber = map["cageNumber"] as? String,
            origin = map["origin"] as? String,
            notes = map["notes"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun reproductionToMap(r: ReproductionEntity) = mapOf(
        "id" to r.id,
        "femaleId" to r.femaleId,
        "femaleTag" to r.femaleTag,
        "maleId" to r.maleId,
        "maleTag" to r.maleTag,
        "matingDate" to r.matingDate,
        "palpationDate" to r.palpationDate,
        "palpationResult" to r.palpationResult,
        "expectedKindlingDate" to r.expectedKindlingDate,
        "actualKindlingDate" to r.actualKindlingDate,
        "bornAliveCount" to r.bornAliveCount,
        "bornDeadCount" to r.bornDeadCount,
        "expectedWeaningDate" to r.expectedWeaningDate,
        "actualWeaningDate" to r.actualWeaningDate,
        "weanedCount" to r.weanedCount,
        "status" to r.status,
        "notes" to r.notes,
        "createdAt" to r.createdAt
    )

    private fun mapToReproduction(map: Map<String, Any>): ReproductionEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return ReproductionEntity(
            id = id,
            femaleId = (map["femaleId"] as? Number)?.toLong() ?: 0L,
            femaleTag = map["femaleTag"] as? String ?: "",
            maleId = (map["maleId"] as? Number)?.toLong(),
            maleTag = map["maleTag"] as? String,
            matingDate = map["matingDate"] as? String ?: "",
            palpationDate = map["palpationDate"] as? String,
            palpationResult = map["palpationResult"] as? Boolean,
            expectedKindlingDate = map["expectedKindlingDate"] as? String,
            actualKindlingDate = map["actualKindlingDate"] as? String,
            bornAliveCount = (map["bornAliveCount"] as? Number)?.toInt() ?: 0,
            bornDeadCount = (map["bornDeadCount"] as? Number)?.toInt() ?: 0,
            expectedWeaningDate = map["expectedWeaningDate"] as? String,
            actualWeaningDate = map["actualWeaningDate"] as? String,
            weanedCount = (map["weanedCount"] as? Number)?.toInt() ?: 0,
            status = map["status"] as? String ?: "EN_COURS",
            notes = map["notes"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun healthLogToMap(h: HealthLogEntity) = mapOf(
        "id" to h.id,
        "animalId" to h.animalId,
        "animalTag" to h.animalTag,
        "diseaseName" to h.diseaseName,
        "symptoms" to h.symptoms,
        "treatmentType" to h.treatmentType,
        "treatmentName" to h.treatmentName,
        "dosage" to h.dosage,
        "status" to h.status,
        "treatmentDate" to h.treatmentDate,
        "nextDueDate" to h.nextDueDate,
        "isMortality" to h.isMortality,
        "deathCount" to h.deathCount,
        "deathCause" to h.deathCause,
        "cost" to h.cost,
        "notes" to h.notes,
        "createdAt" to h.createdAt
    )

    private fun mapToHealthLog(map: Map<String, Any>): HealthLogEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return HealthLogEntity(
            id = id,
            animalId = (map["animalId"] as? Number)?.toLong(),
            animalTag = map["animalTag"] as? String,
            diseaseName = map["diseaseName"] as? String ?: "",
            symptoms = map["symptoms"] as? String,
            treatmentType = map["treatmentType"] as? String ?: "TRAITEMENT",
            treatmentName = map["treatmentName"] as? String ?: "",
            dosage = map["dosage"] as? String,
            status = map["status"] as? String ?: "EN_COURS",
            treatmentDate = map["treatmentDate"] as? String ?: "",
            nextDueDate = map["nextDueDate"] as? String,
            isMortality = map["isMortality"] as? Boolean ?: false,
            deathCount = (map["deathCount"] as? Number)?.toInt() ?: 1,
            deathCause = map["deathCause"] as? String,
            cost = (map["cost"] as? Number)?.toDouble() ?: 0.0,
            notes = map["notes"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun feedStockToMap(s: FeedStockEntity) = mapOf(
        "id" to s.id,
        "feedName" to s.feedName,
        "quantityKg" to s.quantityKg,
        "minThresholdKg" to s.minThresholdKg,
        "unitCost" to s.unitCost,
        "lastUpdated" to s.lastUpdated
    )

    private fun mapToFeedStock(map: Map<String, Any>): FeedStockEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return FeedStockEntity(
            id = id,
            feedName = map["feedName"] as? String ?: "",
            quantityKg = (map["quantityKg"] as? Number)?.toDouble() ?: 0.0,
            minThresholdKg = (map["minThresholdKg"] as? Number)?.toDouble() ?: 10.0,
            unitCost = (map["unitCost"] as? Number)?.toDouble() ?: 0.0,
            lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun feedDistributionToMap(d: FeedDistributionEntity) = mapOf(
        "id" to d.id,
        "feedStockId" to d.feedStockId,
        "feedName" to d.feedName,
        "distributionDate" to d.distributionDate,
        "quantityKg" to d.quantityKg,
        "targetCategory" to d.targetCategory,
        "notes" to d.notes,
        "createdAt" to d.createdAt
    )

    private fun mapToFeedDistribution(map: Map<String, Any>): FeedDistributionEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return FeedDistributionEntity(
            id = id,
            feedStockId = (map["feedStockId"] as? Number)?.toLong() ?: 0L,
            feedName = map["feedName"] as? String ?: "",
            distributionDate = map["distributionDate"] as? String ?: "",
            quantityKg = (map["quantityKg"] as? Number)?.toDouble() ?: 0.0,
            targetCategory = map["targetCategory"] as? String ?: "TOUS",
            notes = map["notes"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun financeLogToMap(f: FinanceLogEntity) = mapOf(
        "id" to f.id,
        "type" to f.type,
        "category" to f.category,
        "amount" to f.amount,
        "transactionDate" to f.transactionDate,
        "description" to f.description,
        "clientName" to f.clientName,
        "itemDetails" to f.itemDetails,
        "quantity" to f.quantity,
        "animalId" to f.animalId,
        "createdAt" to f.createdAt
    )

    private fun mapToFinanceLog(map: Map<String, Any>): FinanceLogEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return FinanceLogEntity(
            id = id,
            type = map["type"] as? String ?: "RECETTE",
            category = map["category"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            transactionDate = map["transactionDate"] as? String ?: "",
            description = map["description"] as? String ?: "",
            clientName = map["clientName"] as? String,
            itemDetails = map["itemDetails"] as? String,
            quantity = (map["quantity"] as? Number)?.toInt(),
            animalId = (map["animalId"] as? Number)?.toLong(),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun taskToMap(t: TaskEntity) = mapOf(
        "id" to t.id,
        "title" to t.title,
        "dueDate" to t.dueDate,
        "dueTime" to t.dueTime,
        "priority" to t.priority,
        "type" to t.type,
        "category" to t.category,
        "reminderInfo" to t.reminderInfo,
        "repeatInfo" to t.repeatInfo,
        "isAuto" to t.isAuto,
        "isFlagged" to t.isFlagged,
        "relatedEntityId" to t.relatedEntityId,
        "isCompleted" to t.isCompleted,
        "completedAt" to t.completedAt,
        "createdAt" to t.createdAt
    )

    private fun mapToTask(map: Map<String, Any>): TaskEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return TaskEntity(
            id = id,
            title = map["title"] as? String ?: "",
            dueDate = map["dueDate"] as? String ?: "",
            dueTime = map["dueTime"] as? String ?: "08:00",
            priority = map["priority"] as? String ?: "MOYENNE",
            type = map["type"] as? String ?: "AUTRE",
            category = map["category"] as? String,
            reminderInfo = map["reminderInfo"] as? String ?: "le jour même",
            repeatInfo = map["repeatInfo"] as? String,
            isAuto = map["isAuto"] as? Boolean ?: false,
            isFlagged = map["isFlagged"] as? Boolean ?: false,
            relatedEntityId = (map["relatedEntityId"] as? Number)?.toLong(),
            isCompleted = map["isCompleted"] as? Boolean ?: false,
            completedAt = (map["completedAt"] as? Number)?.toLong(),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun notificationToMap(n: NotificationEntity) = mapOf(
        "id" to n.id,
        "title" to n.title,
        "message" to n.message,
        "type" to n.type,
        "date" to n.date,
        "time" to n.time,
        "isRead" to n.isRead,
        "createdAt" to n.createdAt
    )

    private fun mapToNotification(map: Map<String, Any>): NotificationEntity? {
        val id = (map["id"] as? Number)?.toLong() ?: return null
        return NotificationEntity(
            id = id,
            title = map["title"] as? String ?: "",
            message = map["message"] as? String ?: "",
            type = map["type"] as? String ?: "INFO",
            date = map["date"] as? String ?: "",
            time = map["time"] as? String ?: "07:00",
            isRead = map["isRead"] as? Boolean ?: false,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }.addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }.addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
