package com.perceptionparadox.oneura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptionparadox.oneura.data.pref.SettingsRepository
import com.perceptionparadox.oneura.data.remote.NetworkModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val baseUrl: StateFlow<String> = repository.baseUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val modelName: StateFlow<String> = repository.modelName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val timeoutSeconds: StateFlow<Long> = repository.timeoutSeconds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 60L
    )

    init {
        // Sync DataStore settings to NetworkModule automatically
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
}
