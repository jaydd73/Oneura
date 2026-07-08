package com.perceptionparadox.oneura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import com.perceptionparadox.oneura.data.repository.CognitiveRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrainViewModel(private val repository: CognitiveRepository) : ViewModel() {

    val reflections: StateFlow<List<ReflectionEntity>> = repository.innerVoiceReflectionStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // For now, keeping dreams separate or as a manual trigger
    val dreams: StateFlow<List<DreamEntity>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    fun reflect() {
        // This could be a manual trigger for a deep reflection session
    }

    fun dream() {
        // Manual dream trigger
    }
}
