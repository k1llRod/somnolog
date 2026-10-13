package com.antigravity.somnolog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigravity.somnolog.data.local.dao.AudioEventDao
import com.antigravity.somnolog.data.local.dao.SessionDao
import com.antigravity.somnolog.data.local.entity.AudioEventEntity
import com.antigravity.somnolog.data.local.entity.SleepSessionEntity

@Database(
    entities = [SleepSessionEntity::class, AudioEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun audioEventDao(): AudioEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "somnolog.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
