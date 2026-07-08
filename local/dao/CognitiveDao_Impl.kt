package com.perceptionparadox.oneura.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.perceptionparadox.oneura.`data`.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.`data`.local.entity.DreamEntity
import com.perceptionparadox.oneura.`data`.local.entity.ReflectionEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CognitiveDao_Impl(
  __db: RoomDatabase,
) : CognitiveDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfChatMessageEntity: EntityInsertAdapter<ChatMessageEntity>

  private val __insertAdapterOfReflectionEntity: EntityInsertAdapter<ReflectionEntity>

  private val __insertAdapterOfDreamEntity: EntityInsertAdapter<DreamEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfChatMessageEntity = object : EntityInsertAdapter<ChatMessageEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `chat_messages` (`id`,`sender`,`content`,`timestamp`,`isOuterVoice`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChatMessageEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.sender)
        statement.bindText(3, entity.content)
        statement.bindLong(4, entity.timestamp)
        val _tmp: Int = if (entity.isOuterVoice) 1 else 0
        statement.bindLong(5, _tmp.toLong())
      }
    }
    this.__insertAdapterOfReflectionEntity = object : EntityInsertAdapter<ReflectionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `reflections` (`id`,`content`,`triggerSource`,`timestamp`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ReflectionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.content)
        statement.bindText(3, entity.triggerSource)
        statement.bindLong(4, entity.timestamp)
      }
    }
    this.__insertAdapterOfDreamEntity = object : EntityInsertAdapter<DreamEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `dreams` (`id`,`sleepStage`,`memoryConsolidationSummary`,`personalityInsightEvolved`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DreamEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.sleepStage)
        statement.bindText(3, entity.memoryConsolidationSummary)
        statement.bindText(4, entity.personalityInsightEvolved)
        statement.bindLong(5, entity.timestamp)
      }
    }
  }

  public override suspend fun insertMessage(message: ChatMessageEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChatMessageEntity.insert(_connection, message)
  }

  public override suspend fun insertReflection(reflection: ReflectionEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfReflectionEntity.insert(_connection, reflection)
  }

  public override suspend fun insertDreamLog(dream: DreamEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfDreamEntity.insert(_connection, dream)
  }

  public override fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>> {
    val _sql: String = "SELECT * FROM chat_messages ORDER BY timestamp ASC"
    return createFlow(__db, false, arrayOf("chat_messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSender: Int = getColumnIndexOrThrow(_stmt, "sender")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsOuterVoice: Int = getColumnIndexOrThrow(_stmt, "isOuterVoice")
        val _result: MutableList<ChatMessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatMessageEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSender: String
          _tmpSender = _stmt.getText(_columnIndexOfSender)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsOuterVoice: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsOuterVoice).toInt()
          _tmpIsOuterVoice = _tmp != 0
          _item = ChatMessageEntity(_tmpId,_tmpSender,_tmpContent,_tmpTimestamp,_tmpIsOuterVoice)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recent(limit: Int): List<ChatMessageEntity> {
    val _sql: String = "SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSender: Int = getColumnIndexOrThrow(_stmt, "sender")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsOuterVoice: Int = getColumnIndexOrThrow(_stmt, "isOuterVoice")
        val _result: MutableList<ChatMessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatMessageEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSender: String
          _tmpSender = _stmt.getText(_columnIndexOfSender)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsOuterVoice: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsOuterVoice).toInt()
          _tmpIsOuterVoice = _tmp != 0
          _item = ChatMessageEntity(_tmpId,_tmpSender,_tmpContent,_tmpTimestamp,_tmpIsOuterVoice)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllReflectionsFlow(): Flow<List<ReflectionEntity>> {
    val _sql: String = "SELECT * FROM reflections ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("reflections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfTriggerSource: Int = getColumnIndexOrThrow(_stmt, "triggerSource")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<ReflectionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReflectionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpTriggerSource: String
          _tmpTriggerSource = _stmt.getText(_columnIndexOfTriggerSource)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = ReflectionEntity(_tmpId,_tmpContent,_tmpTriggerSource,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recentReflections(limit: Int): List<ReflectionEntity> {
    val _sql: String = "SELECT * FROM reflections ORDER BY timestamp DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfTriggerSource: Int = getColumnIndexOrThrow(_stmt, "triggerSource")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<ReflectionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReflectionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpTriggerSource: String
          _tmpTriggerSource = _stmt.getText(_columnIndexOfTriggerSource)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = ReflectionEntity(_tmpId,_tmpContent,_tmpTriggerSource,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllDreamsFlow(): Flow<List<DreamEntity>> {
    val _sql: String = "SELECT * FROM dreams ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("dreams")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSleepStage: Int = getColumnIndexOrThrow(_stmt, "sleepStage")
        val _columnIndexOfMemoryConsolidationSummary: Int = getColumnIndexOrThrow(_stmt,
            "memoryConsolidationSummary")
        val _columnIndexOfPersonalityInsightEvolved: Int = getColumnIndexOrThrow(_stmt,
            "personalityInsightEvolved")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<DreamEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DreamEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSleepStage: String
          _tmpSleepStage = _stmt.getText(_columnIndexOfSleepStage)
          val _tmpMemoryConsolidationSummary: String
          _tmpMemoryConsolidationSummary = _stmt.getText(_columnIndexOfMemoryConsolidationSummary)
          val _tmpPersonalityInsightEvolved: String
          _tmpPersonalityInsightEvolved = _stmt.getText(_columnIndexOfPersonalityInsightEvolved)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              DreamEntity(_tmpId,_tmpSleepStage,_tmpMemoryConsolidationSummary,_tmpPersonalityInsightEvolved,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearOldMemoryLogs(cutoffTime: Long) {
    val _sql: String = "DELETE FROM chat_messages WHERE timestamp < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, cutoffTime)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
