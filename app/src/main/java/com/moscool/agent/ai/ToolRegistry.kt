package com.moscool.agent.ai

import com.moscool.agent.model.ActionType

/**
 * Formal tool registry. The AI can only call registered tools.
 * Each tool has metadata, schema, permission requirements, and safety level.
 */
object ToolRegistry {

    enum class SafetyLevel {
        SAFE,           // No confirmation needed
        SENSITIVE,      // Confirmation in safe mode
        DESTRUCTIVE     // Always needs confirmation
    }

    data class Tool(
        val name: String,
        val actionType: ActionType,
        val description: String,
        val parameters: List<ToolParameter>,
        val safetyLevel: SafetyLevel = SafetyLevel.SAFE,
        val requiredPermissions: List<String> = emptyList()
    )

    data class ToolParameter(
        val name: String,
        val type: String,
        val description: String,
        val required: Boolean = false,
        val enumValues: List<String>? = null
    )

    private val tools = listOf(
        Tool(
            name = "open_app",
            actionType = ActionType.OPEN_APP,
            description = "Open an application by package name or display name",
            parameters = listOf(
                ToolParameter("package_name", "string", "Android package name (e.g., com.facebook.katana)", false),
                ToolParameter("app_name", "string", "Display name of the app (e.g., Facebook)", false)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "close_app",
            actionType = ActionType.CLOSE_APP,
            description = "Close the foreground application",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "tap",
            actionType = ActionType.TAP,
            description = "Tap on a UI element identified by text, content description, or resource ID",
            parameters = listOf(
                ToolParameter("text", "string", "Exact text on the element", false),
                ToolParameter("content_description", "string", "Accessibility content description", false),
                ToolParameter("resource_id", "string", "Android resource ID", false),
                ToolParameter("partial_text", "string", "Partial text match", false),
                ToolParameter("index", "integer", "Index if multiple matches (0-based)", false)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "long_press",
            actionType = ActionType.LONG_PRESS,
            description = "Long-press on a UI element",
            parameters = listOf(
                ToolParameter("text", "string", "Exact text on the element", false),
                ToolParameter("content_description", "string", "Accessibility content description", false)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "type_text",
            actionType = ActionType.TYPE,
            description = "Type text into the currently focused editable field",
            parameters = listOf(
                ToolParameter("text", "string", "Text to type", true)
            ),
            safetyLevel = SafetyLevel.SENSITIVE
        ),
        Tool(
            name = "clear_text",
            actionType = ActionType.CLEAR_TEXT,
            description = "Clear text from the currently focused editable field",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "scroll",
            actionType = ActionType.SCROLL,
            description = "Scroll in a direction",
            parameters = listOf(
                ToolParameter("direction", "string", "Scroll direction", true,
                    listOf("up", "down", "left", "right"))
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "swipe",
            actionType = ActionType.SWIPE,
            description = "Perform a swipe gesture from one point to another",
            parameters = listOf(
                ToolParameter("direction", "string", "Swipe direction", true,
                    listOf("up", "down", "left", "right"))
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "press_back",
            actionType = ActionType.PRESS_BACK,
            description = "Press the system back button",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "press_home",
            actionType = ActionType.PRESS_HOME,
            description = "Press the system home button",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "wait",
            actionType = ActionType.WAIT,
            description = "Wait for a specified number of milliseconds",
            parameters = listOf(
                ToolParameter("duration_ms", "integer", "Milliseconds to wait", true)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "read_screen",
            actionType = ActionType.READ_SCREEN,
            description = "Read the current screen's UI elements and return their information",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "take_screenshot",
            actionType = ActionType.TAKE_SCREENSHOT,
            description = "Take a screenshot of the current screen (requires MediaProjection permission)",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SENSITIVE
        ),
        Tool(
            name = "find_element",
            actionType = ActionType.FIND_ELEMENT,
            description = "Search for a UI element on the current screen without interacting with it",
            parameters = listOf(
                ToolParameter("text", "string", "Text to search for", false),
                ToolParameter("content_description", "string", "Content description to search for", false),
                ToolParameter("resource_id", "string", "Resource ID to search for", false)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "generate_text",
            actionType = ActionType.GENERATE_TEXT,
            description = "Generate social media content using the AI",
            parameters = listOf(
                ToolParameter("topic", "string", "Topic of the content", true),
                ToolParameter("tone", "string", "Tone (e.g., professional, friendly, educational)", false),
                ToolParameter("audience", "string", "Target audience", false),
                ToolParameter("business", "string", "Business or brand name", false),
                ToolParameter("platform", "string", "Target platform (Facebook, Instagram, etc.)", false)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "ask_user",
            actionType = ActionType.ASK_USER,
            description = "Ask the user a question and wait for their response",
            parameters = listOf(
                ToolParameter("question", "string", "Question to ask the user", true)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "verify_action",
            actionType = ActionType.VERIFY,
            description = "Verify that the last action was completed successfully",
            parameters = listOf(
                ToolParameter("expected", "string", "What to verify (e.g., 'text_exists', 'element_visible')", true),
                ToolParameter("target", "string", "The expected value", true)
            ),
            safetyLevel = SafetyLevel.SAFE
        ),
        Tool(
            name = "stop_agent",
            actionType = ActionType.STOP,
            description = "Stop the agent immediately",
            parameters = emptyList(),
            safetyLevel = SafetyLevel.SAFE
        )
    )

    fun getAllTools(): List<Tool> = tools

    fun getToolByName(name: String): Tool? = tools.find { it.name == name }

    fun getAIToolDefinitions(): List<ToolDefinition> = tools.map { tool ->
        ToolDefinition(
            name = tool.name,
            description = tool.description,
            parameters = tool.parameters.associate { param ->
                param.name to ParameterDef(
                    type = param.type,
                    description = param.description,
                    required = param.required,
                    enum = param.enumValues
                )
            }
        )
    }

    fun getToolsRequiringConfirmation(): List<Tool> =
        tools.filter { it.safetyLevel != SafetyLevel.SAFE }
}
