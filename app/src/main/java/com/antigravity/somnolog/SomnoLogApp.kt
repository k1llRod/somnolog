package com.antigravity.somnolog

import android.app.Application
import com.antigravity.somnolog.data.local.AppDatabase
import com.antigravity.somnolog.data.repository.SleepRepositoryImpl

/**
 * Base Application class for SomnoLog.
 * Initializes database and dependencies.
 */
class SomnoLogApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: SleepRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = SleepRepositoryImpl(database)
    }

    companion object {
        lateinit var instance: SomnoLogApp
            private set
    }
}
