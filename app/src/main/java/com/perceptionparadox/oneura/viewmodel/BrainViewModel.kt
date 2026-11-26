package com.perceptionparadox.oneura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import com.perceptionparadox.oneura.data.repository.CognitiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrainViewModel(private val repository: CognitiveRepository) : ViewModel() {

    val reflections: StateFlow<List<ReflectionEntity>> = repository.innerVoiceReflectionStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Ported from the Antigravity copy: dreams now come from Room instead of an empty placeholder.
    val dreams: StateFlow<List<DreamEntity>> = repository.dreamsStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isDreaming = MutableStateFlow(false)
    val isDreaming: StateFlow<Boolean> = _isDreaming.asStateFlow()

    private val _isReflecting = MutableStateFlow(false)
    val isReflecting: StateFlow<Boolean> = _isReflecting.asStateFlow()

    private val _reflectionError = MutableStateFlow<String?>(null)
    val reflectionError: StateFlow<String?> = _reflectionError.asStateFlow()

    fun reflect() {
        if (_isReflecting.value) return
        viewModelScope.launch {
            _isReflecting.value = true
            _reflectionError.value = null
            val result = repository.runMetaReflection()
            _reflectionError.value = result.exceptionOrNull()?.message
            _isReflecting.value = false
        }
    }

    fun dream() {
        if (_isDreaming.value) return
        viewModelScope.launch {
            _isDreaming.value = true
            try {
                repository.triggerDreamConsolidation()
            } finally {
                _isDreaming.value = false
            }
        }
    }
}
