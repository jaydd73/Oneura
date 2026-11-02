package com.perceptionparadox.oneura.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOuterVoice: Boolean = true
)
