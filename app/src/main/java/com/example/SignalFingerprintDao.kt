package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalFingerprintDao {
    @Query("SELECT * FROM signal_fingerprints")
    fun getAllFingerprintsFlow(): Flow<List<SignalFingerprintEntity>>

    @Query("SELECT * FROM signal_fingerprints")
    suspend fun getAllFingerprints(): List<SignalFingerprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFingerprint(fingerprint: SignalFingerprintEntity)

    @Update
    suspend fun updateFingerprint(fingerprint: SignalFingerprintEntity)

    @Delete
    suspend fun deleteFingerprint(fingerprint: SignalFingerprintEntity)
}
