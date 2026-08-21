package com.moscool.agent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey val id: String,
    val command: String,
    val state: String,
    val planJson: String,
    val currentStep: Int,
    val historyJson: String,
    val result: String?,
    val error: String?,
    val createdAt: Long,
    val completedAt: Long?
)
