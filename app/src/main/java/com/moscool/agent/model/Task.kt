package com.moscool.agent.model

import kotlinx.serialization.Serializable

/**
 * Task states for the agent state machine.
 */
enum class TaskState {
    IDLE,
    UNDERSTANDING,
    PLANNING,
    WAITING_FOR_PERMISSION,
    EXECUTING,
    VERIFYING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Automation execution modes.
 */
enum class AutomationMode {
    SAFE,       // Default: asks before all sensitive actions
    ASSISTED,   // Routine auto, pauses for sensitive
    AUTONOMOUS  // User-configured categories auto
}

/**
 * Represents a complete task being executed by the agent.
 */
@Serializable
data class Task(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val state: TaskState = TaskState.IDLE,
    val plan: List<AgentAction> = emptyList(),
    val currentStep: Int = 0,
    val history: List<TaskStepResult> = emptyList(),
    val result: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Serializable
data class TaskStepResult(
    val action: AgentAction,
    val success: Boolean,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A single entry in the conversation memory.
 */
@Serializable
data class ConversationMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
