package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "oui_vendors")
data class OuiEntity(
    @PrimaryKey val macPrefix: String, // e.g. "A4:C1:38"
    val vendorName: String,
    val isHighRisk: Boolean
)

@Dao
interface OuiDao {
    @Query("SELECT * FROM oui_vendors WHERE macPrefix = :prefix LIMIT 1")
    suspend fun getVendorByPrefix(prefix: String): OuiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vendors: List<OuiEntity>)
}

@Database(entities = [OuiEntity::class], version = 1, exportSchema = false)
abstract class OuiDatabase : RoomDatabase() {
    abstract fun ouiDao(): OuiDao

    companion object {
        @Volatile
        private var INSTANCE: OuiDatabase? = null

        fun getDatabase(context: Context): OuiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OuiDatabase::class.java,
                    "oui_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                
                // Pre-populate some known vendors
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = instance.ouiDao()
                    dao.insertAll(listOf(
                        OuiEntity("00:08:E2", "Cisco Systems", false),
                        OuiEntity("24:0A:C4", "Espressif Systems", true),
                        OuiEntity("A4:C1:38", "Espressif Systems", true),
                        OuiEntity("00:1A:E8", "Hangzhou Hikvision", true),
                        OuiEntity("00:12:34", "Dahua Technology", true),
                        OuiEntity("00:25:D3", "Texas Instruments", true),
                        OuiEntity("F4:CB:52", "Texas Instruments", true),
                        OuiEntity("00:14:22", "Dell Inc.", false),
                        OuiEntity("00:1A:11", "Google LLC", false),
                        OuiEntity("00:24:E4", "Apple, Inc.", false)
                    ))
                }
                
                instance
            }
        }
    }
}
