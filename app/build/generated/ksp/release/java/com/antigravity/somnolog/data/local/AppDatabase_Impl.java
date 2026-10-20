package com.antigravity.somnolog.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.antigravity.somnolog.data.local.dao.AudioEventDao;
import com.antigravity.somnolog.data.local.dao.AudioEventDao_Impl;
import com.antigravity.somnolog.data.local.dao.SessionDao;
import com.antigravity.somnolog.data.local.dao.SessionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile SessionDao _sessionDao;

  private volatile AudioEventDao _audioEventDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sleep_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `clipCount` INTEGER NOT NULL, `baselineNoiseDb` REAL NOT NULL, `triggerThresholdDb` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `audio_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `peakDb` REAL NOT NULL, `filePath` TEXT NOT NULL, `note` TEXT, FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_events_sessionId` ON `audio_events` (`sessionId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3c7d8a9bff45f56b49d5fc9ee2b40a7e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `sleep_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `audio_events`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSleepSessions = new HashMap<String, TableInfo.Column>(6);
        _columnsSleepSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepSessions.put("endTime", new TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepSessions.put("clipCount", new TableInfo.Column("clipCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepSessions.put("baselineNoiseDb", new TableInfo.Column("baselineNoiseDb", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSleepSessions.put("triggerThresholdDb", new TableInfo.Column("triggerThresholdDb", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSleepSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSleepSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSleepSessions = new TableInfo("sleep_sessions", _columnsSleepSessions, _foreignKeysSleepSessions, _indicesSleepSessions);
        final TableInfo _existingSleepSessions = TableInfo.read(db, "sleep_sessions");
        if (!_infoSleepSessions.equals(_existingSleepSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "sleep_sessions(com.antigravity.somnolog.data.local.entity.SleepSessionEntity).\n"
                  + " Expected:\n" + _infoSleepSessions + "\n"
                  + " Found:\n" + _existingSleepSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsAudioEvents = new HashMap<String, TableInfo.Column>(7);
        _columnsAudioEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("sessionId", new TableInfo.Column("sessionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("peakDb", new TableInfo.Column("peakDb", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("filePath", new TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAudioEvents.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAudioEvents = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAudioEvents.add(new TableInfo.ForeignKey("sleep_sessions", "CASCADE", "NO ACTION", Arrays.asList("sessionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAudioEvents = new HashSet<TableInfo.Index>(1);
        _indicesAudioEvents.add(new TableInfo.Index("index_audio_events_sessionId", false, Arrays.asList("sessionId"), Arrays.asList("ASC")));
        final TableInfo _infoAudioEvents = new TableInfo("audio_events", _columnsAudioEvents, _foreignKeysAudioEvents, _indicesAudioEvents);
        final TableInfo _existingAudioEvents = TableInfo.read(db, "audio_events");
        if (!_infoAudioEvents.equals(_existingAudioEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "audio_events(com.antigravity.somnolog.data.local.entity.AudioEventEntity).\n"
                  + " Expected:\n" + _infoAudioEvents + "\n"
                  + " Found:\n" + _existingAudioEvents);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3c7d8a9bff45f56b49d5fc9ee2b40a7e", "6440cceee09cb5cb397fcbcb0e86e4b6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "sleep_sessions","audio_events");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `sleep_sessions`");
      _db.execSQL("DELETE FROM `audio_events`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SessionDao.class, SessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AudioEventDao.class, AudioEventDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SessionDao sessionDao() {
    if (_sessionDao != null) {
      return _sessionDao;
    } else {
      synchronized(this) {
        if(_sessionDao == null) {
          _sessionDao = new SessionDao_Impl(this);
        }
        return _sessionDao;
      }
    }
  }

  @Override
  public AudioEventDao audioEventDao() {
    if (_audioEventDao != null) {
      return _audioEventDao;
    } else {
      synchronized(this) {
        if(_audioEventDao == null) {
          _audioEventDao = new AudioEventDao_Impl(this);
        }
        return _audioEventDao;
      }
    }
  }
}
