package com.moscool.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moscool.agent.ai.AIProvider
import com.moscool.agent.ai.OpenAIProvider
import com.moscool.agent.agent.AgentEngine
import com.moscool.agent.automation.AccessibilityController
import com.moscool.agent.automation.AppLauncher
import com.moscool.agent.data.prefs.PreferencesManager
import com.moscool.agent.data.repository.TaskRepository
import com.moscool.agent.model.AIProviderConfig
import com.moscool.agent.model.AutomationMode
import com.moscool.agent.model.SocialPost
import com.moscool.agent.model.Task
import com.moscool.agent.model.TaskState
import com.moscool.agent.ui.agent.AgentScreen
import com.moscool.agent.ui.history.HistoryScreen
import com.moscool.agent.ui.home.HomeScreen
import com.moscool.agent.ui.navigation.Screen
import com.moscool.agent.ui.onboarding.OnboardingScreen
import com.moscool.agent.ui.preview.PreviewScreen
import com.moscool.agent.ui.settings.SettingsScreen
import com.moscool.agent.ui.theme.MoscoolAgentTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── ViewModel ──

class MainViewModel : ViewModel() {
    private val app = MoscoolApplication.instance
    private val prefs = app.preferencesManager
    private val taskRepo = app.taskRepository

    private val accessibilityController: AccessibilityController?
        get() = AccessibilityController.getInstance()

    private val appLauncher = AppLauncher(app)

    private var _aiConfig = MutableStateFlow(AIProviderConfig())
    val aiConfig: StateFlow<AIProviderConfig> = _aiConfig.asStateFlow()

    private var _automationMode = MutableStateFlow(AutomationMode.SAFE)
    val automationMode: StateFlow<AutomationMode> = _automationMode.asStateFlow()

    private var _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private var _debugLogging = MutableStateFlow(false)
    val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()

    private var _visionEnabled = MutableStateFlow(false)
    val visionEnabled: StateFlow<Boolean> = _visionEnabled.asStateFlow()

    private var _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private var agentEngine: AgentEngine? = null

    var statusMessage: StateFlow<String> = MutableStateFlow("")
    var taskState: StateFlow<TaskState> = MutableStateFlow(TaskState.IDLE)
    var timelineEvents: StateFlow<List<AgentEngine.TimelineEvent>> = MutableStateFlow(emptyList())
    var generatedPost: StateFlow<SocialPost?> = MutableStateFlow(null)
    var needsConfirmation: StateFlow<Boolean> = MutableStateFlow(false)
    var confirmationMessage: StateFlow<String> = MutableStateFlow("")

    val taskHistory: StateFlow<List<Task>> = taskRepo.allTasks.stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    init {
        // Load preferences
        viewModelScope.launch {
            prefs.aiConfig.collect { _aiConfig.value = it }
        }
        viewModelScope.launch {
            prefs.automationMode.collect { _automationMode.value = it }
        }
        viewModelScope.launch {
            prefs.onboardingCompleted.collect { _onboardingCompleted.value = it }
        }
        viewModelScope.launch {
            prefs.debugLogging.collect { _debugLogging.value = it }
        }
        viewModelScope.launch {
            prefs.visionEnabled.collect { _visionEnabled.value = it }
        }

        // Create agent engine with fallback for when accessibility service isn't available
        initAgentEngine()
    }

    fun updateAccessibilityStatus() {
        val enabled = AccessibilityController.getInstance() != null
        _accessibilityEnabled.value = enabled
        // Reinitialize engine if accessibility just became available
        if (enabled && agentEngine == null) {
            initAgentEngine()
        }
    }

    private fun initAgentEngine() {
        val ac = AccessibilityController.getInstance() ?: return
        val provider = createProvider(_aiConfig.value)
        agentEngine = AgentEngine(
            aiProvider = provider,
            accessibilityController = ac,
            appLauncher = appLauncher,
            logRepository = app.logRepository
        )
        agentEngine?.let { engine ->
            statusMessage = engine.statusMessage
            taskState = engine.taskState
            timelineEvents = engine.timelineEvents
            generatedPost = engine.generatedPost
            needsConfirmation = engine.needsConfirmation
            confirmationMessage = engine.confirmationMessage
        }
    }

    fun executeCommand(command: String) {
        val engine = agentEngine ?: return
        viewModelScope.launch {
            engine.executeCommand(command, _automationMode.value)
            // Save task if completed
            val task = engine.currentTask.value
            if (task != null && (task.state == TaskState.COMPLETED || task.state == TaskState.FAILED)) {
                taskRepo.saveTask(task)
            }
        }
    }

    fun stopAgent() {
        agentEngine?.stopAgent()
    }

    fun confirmAction() {
        agentEngine?.confirmAction()
    }

    fun denyAction() {
        agentEngine?.denyAction()
    }

    fun approveGeneratedPost(post: SocialPost) {
        agentEngine?.updateGeneratedPost(post)
        agentEngine?.confirmAction()
    }

    fun regeneratePost() {
        agentEngine?.denyAction()
        // Re-execute with regenerate context
        viewModelScope.launch {
            agentEngine?.executeCommand("Regenerate the post", _automationMode.value)
        }
    }

    fun saveAIConfig(config: AIProviderConfig) {
        viewModelScope.launch {
            prefs.saveAIConfig(config)
            _aiConfig.value = config
            // Recreate provider
            val provider = createProvider(config)
            agentEngine = AgentEngine(
                aiProvider = provider,
                accessibilityController = AccessibilityController.getInstance() ?: return@launch,
                appLauncher = appLauncher,
                logRepository = app.logRepository
            )
        }
    }

    fun saveAutomationMode(mode: AutomationMode) {
        viewModelScope.launch {
            prefs.saveAutomationMode(mode)
            _automationMode.value = mode
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            prefs.setOnboardingCompleted()
            _onboardingCompleted.value = true
        }
    }

    fun toggleDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDebugLogging(enabled)
            _debugLogging.value = enabled
        }
    }

    fun toggleVision(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setVisionEnabled(enabled)
            _visionEnabled.value = enabled
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            taskRepo.deleteAllTasks()
        }
    }

    private fun createProvider(config: AIProviderConfig): AIProvider {
        return OpenAIProvider(
            config.copy(apiKey = app.credentialStore.getApiKey().ifBlank { config.apiKey })
        )
    }
}

// ── Activity ──

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val aiConfig by viewModel.aiConfig.collectAsState()
            val automationMode by viewModel.automationMode.collectAsState()
            val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsState()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
            val debugLogging by viewModel.debugLogging.collectAsState()
            val visionEnabled by viewModel.visionEnabled.collectAsState()

            MoscoolAgentTheme {
                var currentScreen by remember { mutableStateOf<Screen>(
                    if (onboardingCompleted) Screen.Home else Screen.Onboarding
                ) }

                // Update accessibility status periodically
                LaunchedEffect(Unit) {
                    viewModel.updateAccessibilityStatus()
                }

                when (currentScreen) {
                    Screen.Onboarding -> {
                        OnboardingScreen(
                            accessibilityEnabled = accessibilityEnabled,
                            onComplete = {
                                viewModel.setOnboardingCompleted()
                                currentScreen = Screen.Home
                            },
                            onConfigureAI = { currentScreen = Screen.Settings }
                        )
                    }
                    else -> {
                        MainScaffold(
                            currentScreen = currentScreen,
                            onNavigate = { currentScreen = it },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScaffold(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    viewModel: MainViewModel
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val taskState by viewModel.taskState.collectAsState()
    val timelineEvents by viewModel.timelineEvents.collectAsState()
    val generatedPost by viewModel.generatedPost.collectAsState()
    val needsConfirmation by viewModel.needsConfirmation.collectAsState()
    val confirmationMessage by viewModel.confirmationMessage.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val automationMode by viewModel.automationMode.collectAsState()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsState()
    val debugLogging by viewModel.debugLogging.collectAsState()
    val visionEnabled by viewModel.visionEnabled.collectAsState()
    val taskHistory by viewModel.taskHistory.collectAsState()

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.Onboarding && currentScreen != Screen.Agent && currentScreen != Screen.Preview) {
                NavigationBar {
                    val items = listOf(
                        Triple(Screen.Home, Icons.Default.Home, "Home"),
                        Triple(Screen.History, Icons.Default.History, "History"),
                        Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                    )
                    items.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentScreen == screen,
                            onClick = { onNavigate(screen) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            Screen.Home -> HomeScreen(
                statusMessage = statusMessage,
                taskState = taskState,
                accessibilityEnabled = accessibilityEnabled,
                aiConfigured = aiConfig.isValid,
                onExecute = { command ->
                    viewModel.executeCommand(command)
                    onNavigate(Screen.Agent)
                },
                onStop = { viewModel.stopAgent() }
            )

            Screen.Agent -> AgentScreen(
                timelineEvents = timelineEvents,
                taskState = taskState,
                onStop = { viewModel.stopAgent() }
            )

            Screen.Preview -> PreviewScreen(
                post = generatedPost,
                onApprove = { post ->
                    viewModel.approveGeneratedPost(post)
                    onNavigate(Screen.Agent)
                },
                onRegenerate = {
                    viewModel.regeneratePost()
                    onNavigate(Screen.Agent)
                },
                onCancel = {
                    viewModel.denyAction()
                    onNavigate(Screen.Agent)
                },
                onBack = { onNavigate(Screen.Agent) }
            )

            Screen.History -> HistoryScreen(
                tasks = taskHistory,
                onClearHistory = { viewModel.clearHistory() }
            )

            Screen.Settings -> SettingsScreen(
                config = aiConfig,
                automationMode = automationMode,
                accessibilityEnabled = accessibilityEnabled,
                debugLogging = debugLogging,
                visionEnabled = visionEnabled,
                versionName = BuildConfig.VERSION_NAME,
                onSaveConfig = { viewModel.saveAIConfig(it) },
                onSaveMode = { viewModel.saveAutomationMode(it) },
                onToggleDebugLogging = { viewModel.toggleDebugLogging(it) },
                onToggleVision = { viewModel.toggleVision(it) },
                onClearHistory = { viewModel.clearHistory() }
            )

            Screen.Onboarding -> { /* Handled above */ }
        }
    }
}


