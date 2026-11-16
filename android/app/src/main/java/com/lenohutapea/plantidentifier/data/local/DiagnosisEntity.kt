package com.lenohutapea.plantidentifier.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved diagnosis. The full API responses are kept as JSON so a history entry
 * can be reopened offline with exactly what was shown the first time.
 */
@Entity(tableName = "diagnoses")
data class DiagnosisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val imagePath: String,
    val plantId: String,
    val plantName: String,
    val plantConfidence: Double,
    val conditionStatus: String,
    val conditionName: String?,
    val conditionConfidence: Double?,
    val predictionJson: String,
    val plantInfoJson: String?,
)
