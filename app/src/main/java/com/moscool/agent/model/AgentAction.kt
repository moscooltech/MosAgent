package com.moscool.agent.model

import kotlinx.serialization.Serializable

/**
 * Represents an action the AI agent can instruct the device to perform.
 */
@Serializable
data class AgentAction(
    val action: ActionType,
    val target: ActionTarget? = null,
    val text: String? = null,
    val reason: String? = null,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
enum class ActionType {
    OPEN_APP,
    CLOSE_APP,
    TAP,
    LONG_PRESS,
    TYPE,
    CLEAR_TEXT,
    SWIPE,
    SCROLL,
    PRESS_BACK,
    PRESS_HOME,
    WAIT,
    READ_SCREEN,
    TAKE_SCREENSHOT,
    FIND_ELEMENT,
    GENERATE_TEXT,
    ASK_USER,
    VERIFY,
    COMPLETE,
    STOP
}

@Serializable
data class ActionTarget(
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    val partialText: String? = null,
    val index: Int = 0,
    val x: Float? = null,
    val y: Float? = null
)

/**
 * The result of executing an action.
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val screenshot: ByteArray? = null,
    val uiSnapshot: UISnapshot? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActionResult) return false
        return success == other.success && message == other.message
    }
    override fun hashCode(): Int = 31 * success.hashCode() + (message?.hashCode() ?: 0)
}

/**
 * Represents the current state of the device screen.
 */
data class UISnapshot(
    val packageName: String? = null,
    val activityName: String? = null,
    val elements: List<UIElementInfo> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class UIElementInfo(
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isEnabled: Boolean = true,
    val bounds: android.graphics.Rect = android.graphics.Rect(),
    val depth: Int = 0,
    val childCount: Int = 0
)
