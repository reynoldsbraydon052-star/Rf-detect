package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDeviceDao {
    @Query("SELECT * FROM ble_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllBleDevices(): Flow<List<BleDeviceEntity>>

    @Query("SELECT * FROM ble_devices WHERE distanceMeters <= :maxDistance ORDER BY distanceMeters ASC")
    fun getBleDevicesWithinDistance(maxDistance: Float): Flow<List<BleDeviceEntity>>

    @Query("SELECT * FROM ble_devices WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getDeviceByMac(macAddress: String): BleDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBleDevice(device: BleDeviceEntity)

    @Query("DELETE FROM ble_devices")
    suspend fun clearAllBleDevices()

    @Query("DELETE FROM ble_devices WHERE macAddress = :macAddress")
    suspend fun deleteBleDevice(macAddress: String)
}
