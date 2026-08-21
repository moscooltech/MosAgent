package com.moscool.agent.agent

import com.moscool.agent.ai.AIProvider
import com.moscool.agent.ai.PromptManager
import com.moscool.agent.ai.ToolRegistry
import com.moscool.agent.model.ActionTarget
import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction
import com.moscool.agent.model.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Uses the AI to convert user commands into structured action plans.
 */
class AgentPlanner(private val aiProvider: AIProvider) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /**
     * Parse the AI response into a list of agent actions.
     * Handles both tool-call and text-based responses.
     */
    fun parseActionsFromResponse(response: String): List<AgentAction> {
        return try {
            // Try parsing as JSON array of actions
            val element = json.parseToJsonElement(response)
            when {
                element is kotlinx.serialization.json.JsonArray -> {
                    element.mapNotNull { parseAction(it.jsonObject) }
                }
                element is JsonObject && element.containsKey("actions") -> {
                    element["actions"]?.jsonArray?.mapNotNull { parseAction(it.jsonObject) } ?: emptyList()
                }
                else -> {
                    // Try to extract actions from text
                    parseActionsFromText(response)
                }
            }
        } catch (_: Exception) {
            parseActionsFromText(response)
        }
    }

    private fun parseAction(obj: JsonObject): AgentAction? {
        return try {
            val actionStr = obj["action"]?.jsonPrimitive?.contentOrNull ?: return null
            val actionType = try {
                ActionType.valueOf(actionStr.uppercase())
            } catch (_: Exception) {
                return null
            }
            val targetObj = obj["target"]?.jsonObject
            val target = targetObj?.let {
                ActionTarget(
                    text = it["text"]?.jsonPrimitive?.contentOrNull,
                    contentDescription = it["content_description"]?.jsonPrimitive?.contentOrNull,
                    resourceId = it["resource_id"]?.jsonPrimitive?.contentOrNull,
                    className = it["class"]?.jsonPrimitive?.contentOrNull,
                    partialText = it["partial_text"]?.jsonPrimitive?.contentOrNull
                )
            }
            AgentAction(
                action = actionType,
                target = target,
                text = obj["text"]?.jsonPrimitive?.contentOrNull,
                reason = obj["reason"]?.jsonPrimitive?.contentOrNull
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse actions from natural language text response.
     */
    private fun parseActionsFromText(text: String): List<AgentAction> {
        val actions = mutableListOf<AgentAction>()
        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val action = when {
                trimmed.contains("OPEN_APP", ignoreCase = true) ||
                trimmed.contains("open app", ignoreCase = true) -> {
                    val appName = extractAfter(trimmed, listOf("open ", "open_app: ", "OPEN_APP: "))
                    AgentAction(
                        action = ActionType.OPEN_APP,
                        target = ActionTarget(packageName = appName?.let { resolvePackageName(it) }),
                        reason = "Open $appName"
                    )
                }
                trimmed.contains("TAP", ignoreCase = true) -> {
                    val target = extractAfter(trimmed, listOf("tap ", "tap: ", "TAP: "))
                    AgentAction(
                        action = ActionType.TAP,
                        target = target?.let { ActionTarget(text = it) },
                        reason = "Tap on $target"
                    )
                }
                trimmed.contains("TYPE", ignoreCase = true) -> {
                    val textToType = extractAfter(trimmed, listOf("type ", "type: ", "TYPE: ", "type_text: "))
                    AgentAction(
                        action = ActionType.TYPE,
                        text = textToType,
                        reason = "Type text"
                    )
                }
                trimmed.contains("SCROLL", ignoreCase = true) -> {
                    val direction = extractAfter(trimmed, listOf("scroll ", "scroll: ", "SCROLL: ")) ?: "down"
                    AgentAction(
                        action = ActionType.SCROLL,
                        parameters = mapOf("direction" to direction),
                        reason = "Scroll $direction"
                    )
                }
                trimmed.contains("PRESS_BACK", ignoreCase = true) ||
                trimmed.contains("press back", ignoreCase = true) -> {
                    AgentAction(action = ActionType.PRESS_BACK, reason = "Press back")
                }
                trimmed.contains("PRESS_HOME", ignoreCase = true) ||
                trimmed.contains("press home", ignoreCase = true) -> {
                    AgentAction(action = ActionType.PRESS_HOME, reason = "Press home")
                }
                trimmed.contains("WAIT", ignoreCase = true) -> {
                    val ms = extractAfter(trimmed, listOf("wait ", "wait: ", "WAIT: "))
                        ?.toLongOrNull() ?: 2000
                    AgentAction(
                        action = ActionType.WAIT,
                        parameters = mapOf("duration_ms" to ms.toString()),
                        reason = "Wait ${ms}ms"
                    )
                }
                trimmed.contains("READ_SCREEN", ignoreCase = true) -> {
                    AgentAction(action = ActionType.READ_SCREEN, reason = "Read screen")
                }
                trimmed.contains("COMPLETE", ignoreCase = true) -> {
                    AgentAction(action = ActionType.COMPLETE, reason = "Task complete")
                }
                else -> null
            }
            action?.let { actions.add(it) }
        }
        return actions
    }

    private fun extractAfter(text: String, prefixes: List<String>): String? {
        for (prefix in prefixes) {
            val idx = text.indexOf(prefix, ignoreCase = true)
            if (idx >= 0) {
                return text.substring(idx + prefix.length).trim().removeSurrounding("\"")
            }
        }
        return null
    }

    private fun resolvePackageName(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("facebook") -> "com.facebook.katana"
            lowerName.contains("telegram") -> "org.telegram.messenger"
            lowerName.contains("instagram") -> "com.instagram.android"
            lowerName.contains("youtube") -> "com.google.android.youtube"
            lowerName.contains("whatsapp") -> "com.whatsapp"
            lowerName.contains("chrome") -> "com.android.chrome"
            lowerName.contains("gmail") -> "com.google.android.gm"
            lowerName.contains("twitter") || lowerName.contains("x") -> "com.twitter.android"
            else -> name
        }
    }
}
