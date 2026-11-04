package com.perceptionparadox.oneura.data.pref

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val modelNameKey = stringPreferencesKey("model_name")
    private val timeoutKey = longPreferencesKey("timeout_seconds")
    private val aiProviderKey = stringPreferencesKey("ai_provider")
    private val geminiApiKey = stringPreferencesKey("gemini_api_key")

    val aiProvider: Flow<String> = context.dataStore.data.map { it[aiProviderKey] ?: "GEMINI" }
    val geminiApi: Flow<String> = context.dataStore.data.map { it[geminiApiKey] ?: "" }

    val baseUrl: Flow<String> = context.dataStore.data.map { 
        var url = (it[baseUrlKey] ?: "http://127.0.0.1:11434/").trim()
        if (url.count { char -> char == ':' } > 2 && url.contains("http", ignoreCase = true)) {
            val secondHttp = url.indexOf("http", startIndex = 7, ignoreCase = true)
            if (secondHttp != -1) url = url.substring(0, secondHttp).trim()
        }
        url
    }
    val modelName: Flow<String> = context.dataStore.data.map { (it[modelNameKey] ?: "tinyllama").trim() }
    val timeoutSeconds: Flow<Long> = context.dataStore.data.map { it[timeoutKey] ?: 120L }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[baseUrlKey] = url.trim() }
    }

    suspend fun saveModelName(name: String) {
        context.dataStore.edit { it[modelNameKey] = name.trim() }
    }

    suspend fun saveTimeout(seconds: Long) {
        context.dataStore.edit { it[timeoutKey] = seconds }
    }

    suspend fun saveAiProvider(provider: String) {
        context.dataStore.edit { it[aiProviderKey] = provider }
    }

    suspend fun saveGeminiKey(key: String) {
        context.dataStore.edit { it[geminiApiKey] = key.trim() }
    }
}
