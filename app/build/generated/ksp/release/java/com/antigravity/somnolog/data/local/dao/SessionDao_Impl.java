package com.antigravity.somnolog.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.antigravity.somnolog.data.local.entity.SleepSessionEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SleepSessionEntity> __insertionAdapterOfSleepSessionEntity;

  private final EntityDeletionOrUpdateAdapter<SleepSessionEntity> __deletionAdapterOfSleepSessionEntity;

  private final EntityDeletionOrUpdateAdapter<SleepSessionEntity> __updateAdapterOfSleepSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementClipCount;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSleepSessionEntity = new EntityInsertionAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sleep_sessions` (`id`,`startTime`,`endTime`,`clipCount`,`baselineNoiseDb`,`triggerThresholdDb`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getEndTime());
        }
        statement.bindLong(4, entity.getClipCount());
        statement.bindDouble(5, entity.getBaselineNoiseDb());
        statement.bindDouble(6, entity.getTriggerThresholdDb());
      }
    };
    this.__deletionAdapterOfSleepSessionEntity = new EntityDeletionOrUpdateAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sleep_sessions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSleepSessionEntity = new EntityDeletionOrUpdateAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sleep_sessions` SET `id` = ?,`startTime` = ?,`endTime` = ?,`clipCount` = ?,`baselineNoiseDb` = ?,`triggerThresholdDb` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getEndTime());
        }
        statement.bindLong(4, entity.getClipCount());
        statement.bindDouble(5, entity.getBaselineNoiseDb());
        statement.bindDouble(6, entity.getTriggerThresholdDb());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementClipCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sleep_sessions SET clipCount = clipCount + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final SleepSessionEntity session,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSleepSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final SleepSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSleepSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final SleepSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSleepSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementClipCount(final long sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementClipCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sessionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementClipCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getSessionById(final long id,
      final Continuation<? super SleepSessionEntity> $completion) {
    final String _sql = "SELECT * FROM sleep_sessions WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfClipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "clipCount");
          final int _cursorIndexOfBaselineNoiseDb = CursorUtil.getColumnIndexOrThrow(_cursor, "baselineNoiseDb");
          final int _cursorIndexOfTriggerThresholdDb = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerThresholdDb");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpClipCount;
            _tmpClipCount = _cursor.getInt(_cursorIndexOfClipCount);
            final float _tmpBaselineNoiseDb;
            _tmpBaselineNoiseDb = _cursor.getFloat(_cursorIndexOfBaselineNoiseDb);
            final float _tmpTriggerThresholdDb;
            _tmpTriggerThresholdDb = _cursor.getFloat(_cursorIndexOfTriggerThresholdDb);
            _result = new SleepSessionEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpClipCount,_tmpBaselineNoiseDb,_tmpTriggerThresholdDb);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveSession(final Continuation<? super SleepSessionEntity> $completion) {
    final String _sql = "SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfClipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "clipCount");
          final int _cursorIndexOfBaselineNoiseDb = CursorUtil.getColumnIndexOrThrow(_cursor, "baselineNoiseDb");
          final int _cursorIndexOfTriggerThresholdDb = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerThresholdDb");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpClipCount;
            _tmpClipCount = _cursor.getInt(_cursorIndexOfClipCount);
            final float _tmpBaselineNoiseDb;
            _tmpBaselineNoiseDb = _cursor.getFloat(_cursorIndexOfBaselineNoiseDb);
            final float _tmpTriggerThresholdDb;
            _tmpTriggerThresholdDb = _cursor.getFloat(_cursorIndexOfTriggerThresholdDb);
            _result = new SleepSessionEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpClipCount,_tmpBaselineNoiseDb,_tmpTriggerThresholdDb);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SleepSessionEntity>> getAllSessions() {
    final String _sql = "SELECT * FROM sleep_sessions ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sleep_sessions"}, new Callable<List<SleepSessionEntity>>() {
      @Override
      @NonNull
      public List<SleepSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfClipCount = CursorUtil.getColumnIndexOrThrow(_cursor, "clipCount");
          final int _cursorIndexOfBaselineNoiseDb = CursorUtil.getColumnIndexOrThrow(_cursor, "baselineNoiseDb");
          final int _cursorIndexOfTriggerThresholdDb = CursorUtil.getColumnIndexOrThrow(_cursor, "triggerThresholdDb");
          final List<SleepSessionEntity> _result = new ArrayList<SleepSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SleepSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpClipCount;
            _tmpClipCount = _cursor.getInt(_cursorIndexOfClipCount);
            final float _tmpBaselineNoiseDb;
            _tmpBaselineNoiseDb = _cursor.getFloat(_cursorIndexOfBaselineNoiseDb);
            final float _tmpTriggerThresholdDb;
            _tmpTriggerThresholdDb = _cursor.getFloat(_cursorIndexOfTriggerThresholdDb);
            _item = new SleepSessionEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpClipCount,_tmpBaselineNoiseDb,_tmpTriggerThresholdDb);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
