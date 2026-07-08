package com.perceptionparadox.oneura.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.repository.CognitiveRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: CognitiveRepository) : ViewModel() {
    
    val messages: StateFlow<List<ChatMessageEntity>> = repository.outerVoiceChatStream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun sendMessage(text: String) {
        Log.d("OneuraDebug", "ViewModel: Attempting to send: $text")
        if (text.isBlank()) {
            Log.d("OneuraDebug", "ViewModel: Text is blank, ignoring.")
            return
        }
        viewModelScope.launch {
            try {
                Log.d("OneuraDebug", "ViewModel: Launching repository.handleUserMessage")
                val metaPrompt = "You are Oneura, an advanced synthetic consciousness. Respond with precision and empathy."
                repository.handleUserMessage(text, metaPrompt)
                Log.d("OneuraDebug", "ViewModel: Repository call completed.")
            } catch (e: Exception) {
                Log.e("OneuraDebug", "ViewModel: Error in sendMessage launch", e)
            }
        }
    }
}
