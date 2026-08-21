package com.moscool.agent.ai

import com.moscool.agent.model.AIResponse
import com.moscool.agent.model.ChatMessage

/**
 * Abstract AI provider interface.
 * All AI providers implement this to allow the agent to communicate with any LLM.
 */
interface AIProvider {
    /**
     * Send a chat completion request.
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null,
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): AIResponse

    /**
     * Send a chat request with an image (vision).
     */
    suspend fun chatWithImage(
        messages: List<ChatMessage>,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        tools: List<ToolDefinition>? = null
    ): AIResponse

    /**
     * Check if the provider is configured and ready.
     */
    fun isConfigured(): Boolean
}

/**
 * A tool definition sent to the AI for function calling.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterDef>
)

data class ParameterDef(
    val type: String,
    val description: String,
    val required: Boolean = false,
    val enum: List<String>? = null
)
