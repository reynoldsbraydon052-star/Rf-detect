package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@com.squareup.moshi.JsonClass(generateAdapter = true)
@Entity(tableName = "rf_annotations")
data class RfAnnotationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val timestampMs: Long,
    val text: String,
    val category: String, // NOTE, OBSERVATION, EXPECTED_ACTIVITY, FOLLOW_UP, IMPORTANT
    val createdAtMs: Long = System.currentTimeMillis(),
    val author: String? = null
)

