package com.moscool.agent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentActionTest {

    @Test
    fun `AgentAction creation with defaults`() {
        val action = AgentAction(action = ActionType.TAP)
        assertEquals(ActionType.TAP, action.action)
        assertEquals(null, action.target)
        assertEquals(null, action.text)
        assertEquals(null, action.reason)
        assertTrue(action.parameters.isEmpty())
    }

    @Test
    fun `AgentAction creation with all fields`() {
        val target = ActionTarget(text = "Button", resourceId = "btn_id")
        val action = AgentAction(
            action = ActionType.TAP,
            target = target,
            text = "Hello",
            reason = "Tap the button",
            parameters = mapOf("key" to "value")
        )
        assertEquals(ActionType.TAP, action.action)
        assertEquals("Button", action.target?.text)
        assertEquals("btn_id", action.target?.resourceId)
        assertEquals("Hello", action.text)
        assertEquals("Tap the button", action.reason)
        assertEquals("value", action.parameters["key"])
    }

    @Test
    fun `ActionTarget creation`() {
        val target = ActionTarget(
            text = "Submit",
            contentDescription = "Submit button",
            resourceId = "com.example:id/submit"
        )
        assertEquals("Submit", target.text)
        assertEquals("Submit button", target.contentDescription)
        assertEquals("com.example:id/submit", target.resourceId)
    }

    @Test
    fun `SocialPost fromFullText`() {
        val post = SocialPost.fromFullText("Hello world!\nThis is a test post.")
        assertEquals("Hello world!\nThis is a test post.", post.fullText)
        assertEquals("", post.hook)
        assertEquals("", post.body)
        assertTrue(post.hashtags.isEmpty())
    }

    @Test
    fun `SocialPost with all fields`() {
        val post = SocialPost(
            hook = "Hook line",
            body = "Body text",
            callToAction = "CTA here",
            hashtags = listOf("tech", "ai"),
            fullText = "Full post text"
        )
        assertEquals("Hook line", post.hook)
        assertEquals("Body text", post.body)
        assertEquals("CTA here", post.callToAction)
        assertEquals(2, post.hashtags.size)
    }

    @Test
    fun `AIProviderConfig default values`() {
        val config = AIProviderConfig()
        assertEquals(AIProviderType.CUSTOM_OPENAI, config.providerType)
        assertEquals("", config.apiKey)
        assertEquals("", config.baseUrl)
        assertEquals("", config.model)
        assertEquals(4096, config.maxTokens)
        assertEquals(0.7, config.temperature, 0.01)
    }

    @Test
    fun `AIProviderConfig isValid`() {
        val invalid = AIProviderConfig()
        assertTrue(!invalid.isValid)

        val valid = AIProviderConfig(apiKey = "sk-test", model = "gpt-4o")
        assertTrue(valid.isValid)
    }

    @Test
    fun `AIProviderConfig defaultBaseUrl`() {
        val openai = AIProviderConfig(providerType = AIProviderType.OPENAI)
        assertEquals("https://api.openai.com/v1", openai.defaultBaseUrl())

        val gemini = AIProviderConfig(providerType = AIProviderType.GEMINI)
        assertEquals("https://generativelanguage.googleapis.com/v1beta", gemini.defaultBaseUrl())

        val groq = AIProviderConfig(providerType = AIProviderType.GROQ)
        assertEquals("https://api.groq.com/openai/v1", groq.defaultBaseUrl())
    }

    @Test
    fun `TaskState has all required states`() {
        val states = TaskState.values()
        assertEquals(10, states.size)
        assertTrue(states.contains(TaskState.IDLE))
        assertTrue(states.contains(TaskState.EXECUTING))
        assertTrue(states.contains(TaskState.COMPLETED))
        assertTrue(states.contains(TaskState.FAILED))
    }

    @Test
    fun `AutomationMode has all modes`() {
        val modes = AutomationMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(AutomationMode.SAFE))
        assertTrue(modes.contains(AutomationMode.ASSISTED))
        assertTrue(modes.contains(AutomationMode.AUTONOMOUS))
    }

    @Test
    fun `ContentTemplate has all templates`() {
        val templates = ContentTemplate.values()
        assertTrue(templates.size >= 8)
        assertTrue(templates.contains(ContentTemplate.EDUCATIONAL))
        assertTrue(templates.contains(ContentTemplate.PROMOTIONAL))
        assertTrue(templates.contains(ContentTemplate.TIPS))
    }
}
