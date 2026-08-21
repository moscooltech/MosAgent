package com.moscool.agent.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moscool.agent.model.AIProviderConfig
import com.moscool.agent.model.AIProviderType
import com.moscool.agent.model.AutomationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moscool_settings")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val AI_PROVIDER_TYPE = stringPreferencesKey("ai_provider_type")
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val AUTOMATION_MODE = stringPreferencesKey("automation_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEBUG_LOGGING = booleanPreferencesKey("debug_logging")
        val VISION_ENABLED = booleanPreferencesKey("vision_enabled")
    }

    val aiConfig: Flow<AIProviderConfig> = context.dataStore.data.map { prefs ->
        AIProviderConfig(
            providerType = try {
                AIProviderType.valueOf(prefs[Keys.AI_PROVIDER_TYPE] ?: AIProviderType.CUSTOM_OPENAI.name)
            } catch (_: Exception) {
                AIProviderType.CUSTOM_OPENAI
            },
            apiKey = prefs[Keys.API_KEY] ?: "",
            baseUrl = prefs[Keys.BASE_URL] ?: "",
            model = prefs[Keys.MODEL] ?: "",
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 4096,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7
        )
    }

    val automationMode: Flow<AutomationMode> = context.dataStore.data.map { prefs ->
        try {
            AutomationMode.valueOf(prefs[Keys.AUTOMATION_MODE] ?: AutomationMode.SAFE.name)
        } catch (_: Exception) {
            AutomationMode.SAFE
        }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val debugLogging: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEBUG_LOGGING] ?: false
    }

    val visionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.VISION_ENABLED] ?: false
    }

    suspend fun saveAIConfig(config: AIProviderConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER_TYPE] = config.providerType.name
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.BASE_URL] = config.baseUrl
            prefs[Keys.MODEL] = config.model
            prefs[Keys.MAX_TOKENS] = config.maxTokens
            prefs[Keys.TEMPERATURE] = config.temperature
        }
    }

    suspend fun saveAutomationMode(mode: AutomationMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTOMATION_MODE] = mode.name
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setDebugLogging(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEBUG_LOGGING] = enabled
        }
    }

    suspend fun setVisionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VISION_ENABLED] = enabled
        }
    }
}
