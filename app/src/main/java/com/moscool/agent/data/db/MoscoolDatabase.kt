package com.moscool.agent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskHistoryEntity::class, LogEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MoscoolDatabase : RoomDatabase() {
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: MoscoolDatabase? = null

        fun getInstance(context: Context): MoscoolDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoscoolDatabase::class.java,
                    "moscool_agent.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
