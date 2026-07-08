package com.perceptionparadox.oneura.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.perceptionparadox.oneura.`data`.local.dao.CognitiveDao
import com.perceptionparadox.oneura.`data`.local.dao.CognitiveDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class OneuraDatabase_Impl : OneuraDatabase() {
  private val _cognitiveDao: Lazy<CognitiveDao> = lazy {
    CognitiveDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "4d8005af22df1f6c61b5e2d64a670687", "2c371767722aced8572e712f26284f03") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isOuterVoice` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `triggerSource` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `dreams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sleepStage` TEXT NOT NULL, `memoryConsolidationSummary` TEXT NOT NULL, `personalityInsightEvolved` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4d8005af22df1f6c61b5e2d64a670687')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `chat_messages`")
        connection.execSQL("DROP TABLE IF EXISTS `reflections`")
        connection.execSQL("DROP TABLE IF EXISTS `dreams`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsChatMessages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChatMessages.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatMessages.put("sender", TableInfo.Column("sender", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatMessages.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChatMessages.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChatMessages.put("isOuterVoice", TableInfo.Column("isOuterVoice", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChatMessages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesChatMessages: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoChatMessages: TableInfo = TableInfo("chat_messages", _columnsChatMessages,
            _foreignKeysChatMessages, _indicesChatMessages)
        val _existingChatMessages: TableInfo = read(connection, "chat_messages")
        if (!_infoChatMessages.equals(_existingChatMessages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chat_messages(com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity).
              | Expected:
              |""".trimMargin() + _infoChatMessages + """
              |
              | Found:
              |""".trimMargin() + _existingChatMessages)
        }
        val _columnsReflections: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsReflections.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReflections.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReflections.put("triggerSource", TableInfo.Column("triggerSource", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsReflections.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysReflections: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesReflections: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoReflections: TableInfo = TableInfo("reflections", _columnsReflections,
            _foreignKeysReflections, _indicesReflections)
        val _existingReflections: TableInfo = read(connection, "reflections")
        if (!_infoReflections.equals(_existingReflections)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |reflections(com.perceptionparadox.oneura.data.local.entity.ReflectionEntity).
              | Expected:
              |""".trimMargin() + _infoReflections + """
              |
              | Found:
              |""".trimMargin() + _existingReflections)
        }
        val _columnsDreams: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDreams.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDreams.put("sleepStage", TableInfo.Column("sleepStage", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDreams.put("memoryConsolidationSummary",
            TableInfo.Column("memoryConsolidationSummary", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDreams.put("personalityInsightEvolved",
            TableInfo.Column("personalityInsightEvolved", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDreams.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDreams: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDreams: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDreams: TableInfo = TableInfo("dreams", _columnsDreams, _foreignKeysDreams,
            _indicesDreams)
        val _existingDreams: TableInfo = read(connection, "dreams")
        if (!_infoDreams.equals(_existingDreams)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |dreams(com.perceptionparadox.oneura.data.local.entity.DreamEntity).
              | Expected:
              |""".trimMargin() + _infoDreams + """
              |
              | Found:
              |""".trimMargin() + _existingDreams)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "chat_messages", "reflections",
        "dreams")
  }

  public override fun clearAllTables() {
    super.performClear(false, "chat_messages", "reflections", "dreams")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CognitiveDao::class, CognitiveDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun cognitiveDao(): CognitiveDao = _cognitiveDao.value
}
