package com.perceptionparadox.oneura.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dreams")
data class DreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sleepStage: String, // "LIGHT", "DEEP", "REM"
    val memoryConsolidationSummary: String,
    val personalityInsightEvolved: String,
    val timestamp: Long = System.currentTimeMillis()
)
