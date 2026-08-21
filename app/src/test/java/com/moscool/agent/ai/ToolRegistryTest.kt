package com.moscool.agent.ai

import com.moscool.agent.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    @Test
    fun `getAllTools returns all registered tools`() {
        val tools = ToolRegistry.getAllTools()
        assertTrue(tools.isNotEmpty())
        assertEquals(18, tools.size)
    }

    @Test
    fun `getToolByName returns correct tool`() {
        val tapTool = ToolRegistry.getToolByName("tap")
        assertNotNull(tapTool)
        assertEquals(ActionType.TAP, tapTool?.actionType)
    }

    @Test
    fun `getToolByName returns null for unknown tool`() {
        val unknown = ToolRegistry.getToolByName("nonexistent")
        assertNull(unknown)
    }

    @Test
    fun `getAIToolDefinitions returns matching count`() {
        val definitions = ToolRegistry.getAIToolDefinitions()
        assertEquals(ToolRegistry.getAllTools().size, definitions.size)
    }

    @Test
    fun `getToolsRequiringConfirmation filters correctly`() {
        val tools = ToolRegistry.getToolsRequiringConfirmation()
        assertTrue(tools.all {
            it.safetyLevel != ToolRegistry.SafetyLevel.SAFE
        })
    }

    @Test
    fun `each tool has required fields`() {
        ToolRegistry.getAllTools().forEach { tool ->
            assertTrue(tool.name.isNotBlank())
            assertTrue(tool.description.isNotBlank())
            assertTrue(tool.parameters.all { it.name.isNotBlank() && it.type.isNotBlank() })
        }
    }

    @Test
    fun `tap tool has correct parameters`() {
        val tapTool = ToolRegistry.getToolByName("tap")
        assertNotNull(tapTool)
        val paramNames = tapTool?.parameters?.map { it.name } ?: emptyList()
        assertTrue("text" in paramNames || "content_description" in paramNames || "resource_id" in paramNames)
    }

    @Test
    fun `type_text tool has text parameter as required`() {
        val typeTool = ToolRegistry.getToolByName("type_text")
        assertNotNull(typeTool)
        val textParam = typeTool?.parameters?.find { it.name == "text" }
        assertNotNull(textParam)
        assertTrue(textParam?.required == true)
    }

    @Test
    fun `scroll tool has direction parameter with enum`() {
        val scrollTool = ToolRegistry.getToolByName("scroll")
        assertNotNull(scrollTool)
        val dirParam = scrollTool?.parameters?.find { it.name == "direction" }
        assertNotNull(dirParam)
        assertTrue(dirParam?.enumValues?.containsAll(listOf("up", "down", "left", "right")) == true)
    }
}
