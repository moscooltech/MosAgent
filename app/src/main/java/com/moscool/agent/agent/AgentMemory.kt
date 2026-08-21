package com.moscool.agent.agent

import com.moscool.agent.model.ConversationMessage
import com.moscool.agent.model.MessageRole
import com.moscool.agent.model.Task

/**
 * Maintains conversational and task context within a session.
 */
class AgentMemory {

    private val conversationHistory = mutableListOf<ConversationMessage>()
    private var currentTask: Task? = null
    private var currentApplication: String? = null
    private var previousActions: MutableList<String> = mutableListOf()

    fun addUserMessage(content: String) {
        conversationHistory.add(ConversationMessage(role = MessageRole.USER, content = content))
    }

    fun addAssistantMessage(content: String) {
        conversationHistory.add(ConversationMessage(role = MessageRole.ASSISTANT, content = content))
    }

    fun addSystemMessage(content: String) {
        conversationHistory.add(ConversationMessage(role = MessageRole.SYSTEM, content = content))
    }

    fun getConversationHistory(): List<ConversationMessage> = conversationHistory.toList()

    fun getRecentMessages(count: Int = 20): List<ConversationMessage> {
        return conversationHistory.takeLast(count)
    }

    fun setCurrentTask(task: Task) {
        currentTask = task
    }

    fun getCurrentTask(): Task? = currentTask

    fun updateCurrentTask(task: Task) {
        currentTask = task
    }

    fun setCurrentApplication(packageName: String?) {
        currentApplication = packageName
    }

    fun getCurrentApplication(): String? = currentApplication

    fun recordAction(actionDescription: String) {
        previousActions.add(actionDescription)
        if (previousActions.size > 50) {
            previousActions = previousActions.takeLast(50).toMutableList()
        }
    }

    fun getPreviousActions(): List<String> = previousActions.toList()

    fun clear() {
        conversationHistory.clear()
        currentTask = null
        currentApplication = null
        previousActions.clear()
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Build context string for the AI about current state.
     */
    fun buildContextString(): String {
        val sb = StringBuilder()
        currentApplication?.let {
            sb.appendLine("Current app: $it")
        }
        if (previousActions.isNotEmpty()) {
            sb.appendLine("Recent actions:")
            previousActions.takeLast(5).forEach { sb.appendLine("  - $it") }
        }
        currentTask?.let {
            sb.appendLine("Current task state: ${it.state}")
            sb.appendLine("Task command: ${it.command}")
            sb.appendLine("Completed steps: ${it.currentStep}/${it.plan.size}")
        }
        return sb.toString()
    }
}
