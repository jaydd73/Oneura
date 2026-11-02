package com.perceptionparadox.oneura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CognitiveDao {

    // Chat Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    // Reflection Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: ReflectionEntity)

    @Query("SELECT * FROM reflections ORDER BY timestamp DESC")
    fun getAllReflectionsFlow(): Flow<List<ReflectionEntity>>

    @Query("SELECT * FROM reflections ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentReflections(limit: Int): List<ReflectionEntity>

    // Dream & Consolidation Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDreamLog(dream: DreamEntity)

    @Query("SELECT * FROM dreams ORDER BY timestamp DESC")
    fun getAllDreamsFlow(): Flow<List<DreamEntity>>
    
    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTime")
    suspend fun clearOldMemoryLogs(cutoffTime: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}
