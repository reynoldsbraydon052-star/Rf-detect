package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RfRecordedEventEntity::class, DeviceIdentityEntity::class, RfSessionEntity::class, RfAnomalyEntity::class, RfPatternEntity::class, AnomalyCorrelationEntity::class, RfAnnotationEntity::class, EvidenceItem::class], version = 8, exportSchema = false)
abstract class RfRecordingDatabase : RoomDatabase() {
    abstract fun rfRecordedEventDao(): RfRecordedEventDao
    abstract fun deviceIdentityDao(): DeviceIdentityDao
    abstract fun rfSessionDao(): RfSessionDao
    abstract fun rfAnomalyDao(): RfAnomalyDao
    abstract fun anomalyCorrelationDao(): AnomalyCorrelationDao
    abstract fun rfPatternDao(): RfPatternDao
    abstract fun rfAnnotationDao(): RfAnnotationDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        @Volatile
        private var INSTANCE: RfRecordingDatabase? = null

        fun getInstance(context: Context): RfRecordingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RfRecordingDatabase::class.java,
                    "rf_recording_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
