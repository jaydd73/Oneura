package com.perceptionparadox.oneura.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.perceptionparadox.oneura.data.local.dao.CognitiveDao
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity

@Database(
    entities = [ChatMessageEntity::class, ReflectionEntity::class, DreamEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OneuraDatabase : RoomDatabase() {
    abstract fun cognitiveDao(): CognitiveDao
}
