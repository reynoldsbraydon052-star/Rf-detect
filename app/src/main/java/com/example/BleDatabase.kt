package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BleDeviceEntity::class], version = 1, exportSchema = false)
abstract class BleDatabase : RoomDatabase() {
    abstract fun bleDeviceDao(): BleDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: BleDatabase? = null

        fun getInstance(context: Context): BleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BleDatabase::class.java,
                    "ble_radar_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
