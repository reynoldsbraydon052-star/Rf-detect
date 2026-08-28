package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM rf_evidence WHERE sessionId = :sessionId")
    suspend fun getEvidenceForSession(sessionId: String): List<EvidenceItem>

    @Insert
    suspend fun insertEvidence(evidence: List<EvidenceItem>)
}
