package com.moscool.agent.data.repository

import com.moscool.agent.data.db.LogDao
import com.moscool.agent.data.db.LogEntryEntity
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: LogDao) {

    val recentLogs: Flow<List<LogEntryEntity>> = dao.getRecentLogs()

    suspend fun log(level: String, tag: String, message: String, taskId: String? = null) {
        dao.insertLog(
            LogEntryEntity(
                level = level,
                tag = tag,
                message = message,
                taskId = taskId
            )
        )
    }

    suspend fun clearLogs() {
        dao.deleteAllLogs()
    }
}
