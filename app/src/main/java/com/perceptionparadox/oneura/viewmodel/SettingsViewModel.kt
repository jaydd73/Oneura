package com.perceptionparadox.oneura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptionparadox.oneura.data.pref.SettingsRepository
import com.perceptionparadox.oneura.data.remote.NetworkModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _isScanning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val baseUrl: StateFlow<String> = repository.baseUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val modelName: StateFlow<String> = repository.modelName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val timeoutSeconds: StateFlow<Long> = repository.timeoutSeconds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 120L
    )

    val aiProvider: StateFlow<String> = repository.aiProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "GEMINI"
    )

    val geminiApiKey: StateFlow<String> = repository.geminiApi.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val xaiApiKey: StateFlow<String> = repository.xaiApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val xaiCompanionModel: StateFlow<String> = repository.xaiCompanionModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    val xaiBackgroundModel: StateFlow<String> = repository.xaiBackgroundModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    init {
        viewModelScope.launch {
            combine(repository.baseUrl, repository.timeoutSeconds) { url, timeout ->
                Pair(url, timeout)
            }.collectLatest { (url, timeout) ->
                NetworkModule.updateConfig(url, timeout)
            }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch { repository.saveBaseUrl(url) }
    }

    fun setModelName(name: String) {
        viewModelScope.launch { repository.saveModelName(name) }
    }

    fun setTimeout(seconds: Long) {
        viewModelScope.launch { repository.saveTimeout(seconds) }
    }

    fun setAiProvider(provider: String) {
        viewModelScope.launch { repository.saveAiProvider(provider) }
    }

    fun setGeminiKey(key: String) {
        viewModelScope.launch { repository.saveGeminiKey(key) }
    }

    fun setXaiKey(key: String) {
        viewModelScope.launch { repository.saveXaiKey(key) }
    }

    fun setXaiCompanionModel(model: String) {
        viewModelScope.launch { repository.saveXaiCompanionModel(model) }
    }

    fun setXaiBackgroundModel(model: String) {
        viewModelScope.launch { repository.saveXaiBackgroundModel(model) }
    }

    fun runSmartDiscovery() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val engine = com.perceptionparadox.oneura.data.remote.SmartConfigEngine { url, model ->
                    setBaseUrl(url)
                    model?.let { setModelName(it) }
                    setAiProvider("OLLAMA")
                }
                engine.runDiscovery()
            } finally {
                _isScanning.value = false
            }
        }
    }
}
