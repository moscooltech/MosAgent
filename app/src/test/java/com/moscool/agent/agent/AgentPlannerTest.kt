package com.moscool.agent.agent

import com.moscool.agent.model.ActionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentPlannerTest {

    private lateinit var planner: AgentPlanner

    @Before
    fun setup() {
        // AgentPlanner requires an AIProvider, but we only test parseActionsFromResponse
        // which is a pure function. We'll create a minimal mock.
        planner = AgentPlanner(object : com.moscool.agent.ai.AIProvider {
            override suspend fun chat(
                messages: List<com.moscool.agent.model.ChatMessage>,
                tools: List<com.moscool.agent.ai.ToolDefinition>?,
                temperature: Double,
                maxTokens: Int
            ) = com.moscool.agent.model.AIResponse(content = "")

            override suspend fun chatWithImage(
                messages: List<com.moscool.agent.model.ChatMessage>,
                imageBase64: String,
                mimeType: String,
                tools: List<com.moscool.agent.ai.ToolDefinition>?
            ) = com.moscool.agent.model.AIResponse(content = "")

            override fun isConfigured() = true
        })
    }

    @Test
    fun `parseActionsFromResponse parses JSON array`() = runTest {
        val json = """
            [
                {"action": "OPEN_APP", "target": {"text": "Facebook"}, "reason": "Open Facebook"},
                {"action": "TAP", "target": {"text": "Create post"}, "reason": "Tap create post"}
            ]
        """.trimIndent()

        val actions = planner.parseActionsFromResponse(json)
        assertEquals(2, actions.size)
        assertEquals(ActionType.OPEN_APP, actions[0].action)
        assertEquals(ActionType.TAP, actions[1].action)
    }

    @Test
    fun `parseActionsFromResponse parses text-based actions`() = runTest {
        val text = "OPEN_APP: Facebook\nTAP: Create post\nTYPE: Hello world\nPRESS_BACK"
        val actions = planner.parseActionsFromResponse(text)
        assertTrue(actions.isNotEmpty())
        assertEquals(ActionType.OPEN_APP, actions[0].action)
    }

    @Test
    fun `parseActionsFromResponse handles empty input`() = runTest {
        val actions = planner.parseActionsFromResponse("")
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `parseActionsFromResponse handles invalid JSON`() = runTest {
        val actions = planner.parseActionsFromResponse("not valid json {{{")
        // Should fall back to text parsing
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `parseActionsFromResponse parses nested actions object`() = runTest {
        val json = """
            {
                "actions": [
                    {"action": "OPEN_APP", "target": {"text": "Telegram"}}
                ]
            }
        """.trimIndent()

        val actions = planner.parseActionsFromResponse(json)
        assertEquals(1, actions.size)
        assertEquals(ActionType.OPEN_APP, actions[0].action)
    }
}
