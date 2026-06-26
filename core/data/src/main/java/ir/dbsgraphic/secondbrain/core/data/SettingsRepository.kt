package ir.dbsgraphic.secondbrain.core.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.dbsgraphic.secondbrain.core.security.KeystoreCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How the app picks light vs dark. Defaults to following the system. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Optional AI assistant configuration. Disabled by default (§12: AI only
 * suggests, never decides; the app is fully usable with it off). Provider-
 * agnostic: any OpenAI-compatible endpoint (OpenAI / Gemini / Anthropic compat
 * / local) works by setting the base URL, key and model names.
 */
data class AiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val chatModel: String = "gpt-4o-mini",
    val transcribeModel: String = "whisper-1",
) {
    /** Usable only when enabled and minimally configured. */
    val isReady: Boolean get() = enabled && apiKey.isNotBlank() && baseUrl.isNotBlank()
}

/**
 * User preferences (theme, onboarding, AI). Stored locally with DataStore — no
 * network, no cloud (Constitution §10, §11).
 */
interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun observeOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(complete: Boolean)

    fun observeAiConfig(): Flow<AiConfig>
    suspend fun setAiEnabled(enabled: Boolean)
    suspend fun setAiConfig(baseUrl: String, apiKey: String, chatModel: String, transcribeModel: String)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sb_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreCipher: KeystoreCipher,
) : SettingsRepository {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiBaseUrlKey = stringPreferencesKey("ai_base_url")
    private val aiApiKeyKey = stringPreferencesKey("ai_api_key")
    private val aiChatModelKey = stringPreferencesKey("ai_chat_model")
    private val aiTranscribeModelKey = stringPreferencesKey("ai_transcribe_model")

    override fun observeThemeMode(): Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    override fun observeOnboardingComplete(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingKey] ?: false
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingKey] = complete }
    }

    override fun observeAiConfig(): Flow<AiConfig> = context.dataStore.data.map { prefs ->
        val defaults = AiConfig()
        AiConfig(
            enabled = prefs[aiEnabledKey] ?: false,
            baseUrl = prefs[aiBaseUrlKey] ?: defaults.baseUrl,
            apiKey = decryptApiKey(prefs[aiApiKeyKey]),
            chatModel = prefs[aiChatModelKey] ?: defaults.chatModel,
            transcribeModel = prefs[aiTranscribeModelKey] ?: defaults.transcribeModel,
        )
    }

    override suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[aiEnabledKey] = enabled }
    }

    override suspend fun setAiConfig(
        baseUrl: String,
        apiKey: String,
        chatModel: String,
        transcribeModel: String,
    ) {
        context.dataStore.edit {
            it[aiBaseUrlKey] = baseUrl.trim()
            it[aiApiKeyKey] = encryptApiKey(apiKey.trim())
            it[aiChatModelKey] = chatModel.trim()
            it[aiTranscribeModelKey] = transcribeModel.trim()
        }
    }

    // The API key is a personal secret — sealed with a Keystore key, never
    // stored in plaintext (Constitution §11).
    private fun encryptApiKey(plain: String): String =
        if (plain.isEmpty()) "" else Base64.encodeToString(keystoreCipher.encrypt(plain.toByteArray()), Base64.NO_WRAP)

    private fun decryptApiKey(stored: String?): String =
        if (stored.isNullOrEmpty()) {
            ""
        } else {
            runCatching {
                keystoreCipher.decrypt(Base64.decode(stored, Base64.NO_WRAP)).decodeToString()
            }.getOrDefault("")
        }
}
