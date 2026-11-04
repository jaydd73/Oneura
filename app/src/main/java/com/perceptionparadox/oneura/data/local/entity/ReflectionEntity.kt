package com.perceptionparadox.oneura.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val triggerSource: String, // e.g., "USER_CHAT_PROMPT", "CRITIC_AGENT"
    val timestamp: Long = System.currentTimeMillis()
)
