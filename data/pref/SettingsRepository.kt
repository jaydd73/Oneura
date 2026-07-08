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

    val baseUrl: Flow<String> = context.dataStore.data.map { it[baseUrlKey] ?: "http://10.63.132.14:8000/" }
    val modelName: Flow<String> = context.dataStore.data.map { it[modelNameKey] ?: "phi3.5" }
    val timeoutSeconds: Flow<Long> = context.dataStore.data.map { it[timeoutKey] ?: 60L }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[baseUrlKey] = url }
    }

    suspend fun saveModelName(name: String) {
        context.dataStore.edit { it[modelNameKey] = name }
    }

    suspend fun saveTimeout(seconds: Long) {
        context.dataStore.edit { it[timeoutKey] = seconds }
    }
}
