package com.moscool.agent.ai

import com.moscool.agent.model.AIProviderConfig
import com.moscool.agent.model.AIResponse
import com.moscool.agent.model.ChatMessage
import com.moscool.agent.model.ToolCall
import com.moscool.agent.model.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible provider that works with OpenAI, OpenRouter, Groq,
 * Gemini (via OpenAI compat), and any custom OpenAI-compatible endpoint.
 */
class OpenAIProvider(private val config: AIProviderConfig) : AIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    override fun isConfigured(): Boolean = config.isValid

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>?,
        temperature: Double,
        maxTokens: Int
    ): AIResponse = withContext(Dispatchers.IO) {
        val requestBody = buildChatRequest(messages, tools, temperature, maxTokens)
        executeRequest(requestBody)
    }

    override suspend fun chatWithImage(
        messages: List<ChatMessage>,
        imageBase64: String,
        mimeType: String,
        tools: List<ToolDefinition>?
    ): AIResponse = withContext(Dispatchers.IO) {
        val requestBody = buildVisionRequest(messages, imageBase64, mimeType, tools)
        executeRequest(requestBody)
    }

    private fun buildChatRequest(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>?,
        temperature: Double,
        maxTokens: Int
    ): String {
        val body = buildJsonObject {
            put("model", JsonPrimitive(config.model))
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", JsonPrimitive(msg.role))
                        put("content", JsonPrimitive(msg.content))
                    })
                }
            })
            put("temperature", JsonPrimitive(temperature))
            put("max_tokens", JsonPrimitive(maxTokens))
            if (!tools.isNullOrEmpty()) {
                put("tools", buildJsonArray {
                    tools.forEach { tool ->
                        add(buildJsonObject {
                            put("type", JsonPrimitive("function"))
                            put("function", buildJsonObject {
                                put("name", JsonPrimitive(tool.name))
                                put("description", JsonPrimitive(tool.description))
                                put("parameters", buildJsonObject {
                                    put("type", JsonPrimitive("object"))
                                    val props = buildJsonObject {
                                        tool.parameters.forEach { (key, param) ->
                                            put(key, buildJsonObject {
                                                put("type", JsonPrimitive(param.type))
                                                put("description", JsonPrimitive(param.description))
                                                if (!param.enum.isNullOrEmpty()) {
                                                    put("enum", buildJsonArray {
                                                        param.enum.forEach { add(JsonPrimitive(it)) }
                                                    })
                                                }
                                            })
                                        }
                                    }
                                    put("properties", props)
                                    val required = buildJsonArray {
                                        tool.parameters.filter { it.value.required }.forEach {
                                            add(JsonPrimitive(it.key))
                                        }
                                    }
                                    put("required", required)
                                })
                            })
                        })
                    }
                })
            }
        }
        return body.toString()
    }

    private fun buildVisionRequest(
        messages: List<ChatMessage>,
        imageBase64: String,
        mimeType: String,
        tools: List<ToolDefinition>?
    ): String {
        val body = buildJsonObject {
            put("model", JsonPrimitive(config.model))
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    if (msg.role == "user") {
                        add(buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("text"))
                                    put("text", JsonPrimitive(msg.content))
                                })
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("image_url"))
                                    put("image_url", buildJsonObject {
                                        put("url", JsonPrimitive("data:$mimeType;base64,$imageBase64"))
                                    })
                                })
                            })
                        })
                    } else {
                        add(buildJsonObject {
                            put("role", JsonPrimitive(msg.role))
                            put("content", JsonPrimitive(msg.content))
                        })
                    }
                }
            })
            put("temperature", JsonPrimitive(0.7))
            put("max_tokens", JsonPrimitive(4096))
            if (!tools.isNullOrEmpty()) {
                put("tools", buildJsonArray {
                    tools.forEach { tool ->
                        add(buildJsonObject {
                            put("type", JsonPrimitive("function"))
                            put("function", buildJsonObject {
                                put("name", JsonPrimitive(tool.name))
                                put("description", JsonPrimitive(tool.description))
                            })
                        })
                    }
                })
            }
        }
        return body.toString()
    }

    private fun executeRequest(body: String): AIResponse {
        val baseUrl = config.effectiveBaseUrl().trimEnd('/')
        val url = "$baseUrl/chat/completions"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from AI provider")

        if (!response.isSuccessful) {
            throw Exception("AI provider error ${response.code}: $responseBody")
        }

        return parseResponse(responseBody)
    }

    private fun parseResponse(responseBody: String): AIResponse {
        val jsonResp = json.parseToJsonElement(responseBody).jsonObject
        val choices = jsonResp["choices"]?.jsonArray ?: return AIResponse(content = "")
        if (choices.isEmpty()) return AIResponse(content = "")

        val choice = choices[0].jsonObject
        val message = choice["message"]?.jsonObject ?: return AIResponse(content = "")
        val finishReason = choice["finish_reason"]?.jsonPrimitive?.contentOrNull

        val content = message["content"]?.jsonPrimitive?.contentOrNull ?: ""

        val toolCalls = message["tool_calls"]?.jsonArray?.mapNotNull { element ->
            try {
                val tc = element.jsonObject
                val function = tc["function"]?.jsonObject ?: return@mapNotNull null
                val name = function["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val argsStr = function["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}"
                val args = try {
                    json.parseToJsonElement(argsStr).jsonObject.mapValues { it.value }
                } catch (_: Exception) {
                    emptyMap()
                }
                ToolCall(
                    id = tc["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    name = name,
                    arguments = args
                )
            } catch (_: Exception) {
                null
            }
        } ?: emptyList()

        val usage = jsonResp["usage"]?.jsonObject?.let { u ->
            TokenUsage(
                promptTokens = u["prompt_tokens"]?.jsonPrimitive?.int ?: 0,
                completionTokens = u["completion_tokens"]?.jsonPrimitive?.int ?: 0,
                totalTokens = u["total_tokens"]?.jsonPrimitive?.int ?: 0
            )
        }

        return AIResponse(
            content = content,
            toolCalls = toolCalls,
            finishReason = finishReason,
            usage = usage
        )
    }
}
