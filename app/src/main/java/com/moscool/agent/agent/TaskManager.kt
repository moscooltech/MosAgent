package com.moscool.agent.agent

import com.moscool.agent.model.AgentAction
import com.moscool.agent.model.Task
import com.moscool.agent.model.TaskState
import com.moscool.agent.model.TaskStepResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the task state machine and task lifecycle.
 */
class TaskManager {

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val _taskState = MutableStateFlow(TaskState.IDLE)
    val taskState: StateFlow<TaskState> = _taskState.asStateFlow()

    fun createTask(command: String): Task {
        val task = Task(command = command, state = TaskState.UNDERSTANDING)
        _currentTask.value = task
        _taskState.value = TaskState.UNDERSTANDING
        return task
    }

    fun updateState(state: TaskState) {
        _taskState.value = state
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(state = state)
        }
    }

    fun setPlan(plan: List<AgentAction>) {
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(
                plan = plan,
                state = TaskState.EXECUTING,
                currentStep = 0
            )
            _taskState.value = TaskState.EXECUTING
        }
    }

    fun recordStepResult(action: AgentAction, success: Boolean, message: String? = null) {
        _currentTask.value?.let { task ->
            val stepResult = TaskStepResult(action = action, success = success, message = message)
            val newHistory = task.history + stepResult
            _currentTask.value = task.copy(
                history = newHistory,
                currentStep = task.currentStep + 1
            )
        }
    }

    fun completeTask(result: String? = null) {
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(
                state = TaskState.COMPLETED,
                result = result,
                completedAt = System.currentTimeMillis()
            )
            _taskState.value = TaskState.COMPLETED
        }
    }

    fun failTask(error: String) {
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(
                state = TaskState.FAILED,
                error = error,
                completedAt = System.currentTimeMillis()
            )
            _taskState.value = TaskState.FAILED
        }
    }

    fun cancelTask() {
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(
                state = TaskState.CANCELLED,
                completedAt = System.currentTimeMillis()
            )
            _taskState.value = TaskState.CANCELLED
        }
    }

    fun waitForConfirmation() {
        _taskState.value = TaskState.WAITING_FOR_CONFIRMATION
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(state = TaskState.WAITING_FOR_CONFIRMATION)
        }
    }

    fun waitForPermission() {
        _taskState.value = TaskState.WAITING_FOR_PERMISSION
        _currentTask.value?.let { task ->
            _currentTask.value = task.copy(state = TaskState.WAITING_FOR_PERMISSION)
        }
    }

    fun reset() {
        _currentTask.value = null
        _taskState.value = TaskState.IDLE
    }

    fun getCurrentPlan(): List<AgentAction> {
        return _currentTask.value?.plan ?: emptyList()
    }

    fun getCurrentStepIndex(): Int {
        return _currentTask.value?.currentStep ?: 0
    }

    fun getNextAction(): AgentAction? {
        val task = _currentTask.value ?: return null
        val step = task.currentStep
        return if (step < task.plan.size) task.plan[step] else null
    }
}
