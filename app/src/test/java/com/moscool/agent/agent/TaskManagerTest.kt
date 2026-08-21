package com.moscool.agent.agent

import com.moscool.agent.model.ActionType
import com.moscool.agent.model.AgentAction
import com.moscool.agent.model.TaskState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskManagerTest {

    private lateinit var taskManager: TaskManager

    @Before
    fun setup() {
        taskManager = TaskManager()
    }

    @Test
    fun `createTask sets state to UNDERSTANDING`() = runTest {
        val task = taskManager.createTask("Open Facebook")
        assertEquals("Open Facebook", task.command)
        assertEquals(TaskState.UNDERSTANDING, task.state)
        assertEquals(TaskState.UNDERSTANDING, taskManager.taskState.value)
    }

    @Test
    fun `setPlan transitions to EXECUTING`() = runTest {
        taskManager.createTask("Test task")
        val plan = listOf(
            AgentAction(action = ActionType.OPEN_APP),
            AgentAction(action = ActionType.TAP)
        )
        taskManager.setPlan(plan)
        assertEquals(TaskState.EXECUTING, taskManager.taskState.value)
        assertEquals(2, taskManager.getCurrentPlan().size)
    }

    @Test
    fun `completeTask sets state to COMPLETED`() = runTest {
        taskManager.createTask("Test task")
        taskManager.completeTask("Done!")
        assertEquals(TaskState.COMPLETED, taskManager.taskState.value)
        assertNotNull(taskManager.currentTask.value?.completedAt)
    }

    @Test
    fun `failTask sets state to FAILED with error`() = runTest {
        taskManager.createTask("Test task")
        taskManager.failTask("Something went wrong")
        assertEquals(TaskState.FAILED, taskManager.taskState.value)
        assertEquals("Something went wrong", taskManager.currentTask.value?.error)
    }

    @Test
    fun `cancelTask sets state to CANCELLED`() = runTest {
        taskManager.createTask("Test task")
        taskManager.cancelTask()
        assertEquals(TaskState.CANCELLED, taskManager.taskState.value)
    }

    @Test
    fun `waitForConfirmation sets state`() = runTest {
        taskManager.createTask("Test task")
        taskManager.waitForConfirmation()
        assertEquals(TaskState.WAITING_FOR_CONFIRMATION, taskManager.taskState.value)
    }

    @Test
    fun `recordStepResult increments currentStep`() = runTest {
        taskManager.createTask("Test task")
        val plan = listOf(
            AgentAction(action = ActionType.OPEN_APP),
            AgentAction(action = ActionType.TAP)
        )
        taskManager.setPlan(plan)
        taskManager.recordStepResult(plan[0], true, "Success")
        assertEquals(1, taskManager.currentTask.value?.currentStep)
        assertEquals(1, taskManager.currentTask.value?.history?.size)
    }

    @Test
    fun `getNextAction returns correct action`() = runTest {
        taskManager.createTask("Test task")
        val plan = listOf(
            AgentAction(action = ActionType.OPEN_APP),
            AgentAction(action = ActionType.TAP)
        )
        taskManager.setPlan(plan)
        val next = taskManager.getNextAction()
        assertNotNull(next)
        assertEquals(ActionType.OPEN_APP, next?.action)

        taskManager.recordStepResult(plan[0], true)
        val nextAfterStep = taskManager.getNextAction()
        assertNotNull(nextAfterStep)
        assertEquals(ActionType.TAP, nextAfterStep?.action)
    }

    @Test
    fun `getNextAction returns null when plan is exhausted`() = runTest {
        taskManager.createTask("Test task")
        val plan = listOf(AgentAction(action = ActionType.OPEN_APP))
        taskManager.setPlan(plan)
        taskManager.recordStepResult(plan[0], true)
        assertNull(taskManager.getNextAction())
    }

    @Test
    fun `reset clears state`() = runTest {
        taskManager.createTask("Test task")
        taskManager.reset()
        assertNull(taskManager.currentTask.value)
        assertEquals(TaskState.IDLE, taskManager.taskState.value)
    }
}
