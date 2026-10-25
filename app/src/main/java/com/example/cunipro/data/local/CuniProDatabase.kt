package com.example.cunipro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cunipro.data.local.dao.*
import com.example.cunipro.data.local.entity.*

@Database(
    entities = [
        AnimalEntity::class,
        ReproductionEntity::class,
        FeedStockEntity::class,
        FeedDistributionEntity::class,
        HealthLogEntity::class,
        FinanceLogEntity::class,
        TaskEntity::class,
        NotificationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class CuniProDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao
    abstract fun reproductionDao(): ReproductionDao
    abstract fun feedDao(): FeedDao
    abstract fun healthDao(): HealthDao
    abstract fun financeDao(): FinanceDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: CuniProDatabase? = null

        fun getDatabase(context: Context): CuniProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CuniProDatabase::class.java,
                    "cunipro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
