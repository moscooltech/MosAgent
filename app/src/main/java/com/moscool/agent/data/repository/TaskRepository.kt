package com.moscool.agent.data.repository

import com.moscool.agent.data.db.TaskHistoryDao
import com.moscool.agent.data.db.TaskHistoryEntity
import com.moscool.agent.model.Task
import com.moscool.agent.model.TaskState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepository(private val dao: TaskHistoryDao) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    val allTasks: Flow<List<Task>> = dao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveTask(task: Task) {
        dao.insertTask(task.toEntity())
    }

    suspend fun getTaskById(id: String): Task? {
        return dao.getTaskById(id)?.toDomain()
    }

    suspend fun deleteAllTasks() {
        dao.deleteAllTasks()
    }

    suspend fun deleteTask(id: String) {
        dao.deleteTaskById(id)
    }

    private fun Task.toEntity() = TaskHistoryEntity(
        id = id,
        command = command,
        state = state.name,
        planJson = json.encodeToString(plan),
        currentStep = currentStep,
        historyJson = json.encodeToString(history),
        result = result,
        error = error,
        createdAt = createdAt,
        completedAt = completedAt
    )

    private fun TaskHistoryEntity.toDomain() = Task(
        id = id,
        command = command,
        state = try { TaskState.valueOf(state) } catch (_: Exception) { TaskState.IDLE },
        plan = try { json.decodeFromString(planJson) } catch (_: Exception) { emptyList() },
        currentStep = currentStep,
        history = try { json.decodeFromString(historyJson) } catch (_: Exception) { emptyList() },
        result = result,
        error = error,
        createdAt = createdAt,
        completedAt = completedAt
    )
}
