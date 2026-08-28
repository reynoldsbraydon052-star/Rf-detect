package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class FloatArrayConverter {
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        return array?.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        if (value.isNullOrEmpty()) return null
        return try {
            value.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            null
        }
    }
}

@Database(entities = [AiMemoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(FloatArrayConverter::class)
abstract class AiMemoryDatabase : RoomDatabase() {
    abstract fun aiMemoryDao(): AiMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AiMemoryDatabase? = null

        fun getDatabase(context: Context): AiMemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiMemoryDatabase::class.java,
                    "ai_memory_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
