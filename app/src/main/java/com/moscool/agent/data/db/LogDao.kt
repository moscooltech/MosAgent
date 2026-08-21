package com.moscool.agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 500): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insertLog(entry: LogEntryEntity)

    @Query("DELETE FROM log_entries")
    suspend fun deleteAllLogs()
}
