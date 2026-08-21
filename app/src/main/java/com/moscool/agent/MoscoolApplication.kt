package com.moscool.agent

import android.app.Application
import com.moscool.agent.data.db.MoscoolDatabase
import com.moscool.agent.data.prefs.PreferencesManager
import com.moscool.agent.data.secure.CredentialStore
import com.moscool.agent.data.repository.LogRepository
import com.moscool.agent.data.repository.TaskRepository

class MoscoolApplication : Application() {

    lateinit var database: MoscoolDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var credentialStore: CredentialStore
        private set
    lateinit var taskRepository: TaskRepository
        private set
    lateinit var logRepository: LogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MoscoolDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)
        credentialStore = CredentialStore(this)
        taskRepository = TaskRepository(database.taskHistoryDao())
        logRepository = LogRepository(database.logDao())
    }

    companion object {
        lateinit var instance: MoscoolApplication
            private set
    }
}
