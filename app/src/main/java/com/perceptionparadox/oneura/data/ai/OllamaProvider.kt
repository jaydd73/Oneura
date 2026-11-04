package com.perceptionparadox.oneura.data.ai

import com.perceptionparadox.oneura.data.remote.NetworkModule
import com.perceptionparadox.oneura.data.remote.model.ChatRequest
import com.perceptionparadox.oneura.data.remote.model.NetworkMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OllamaProvider(private val targetModel: String) : AiProvider {
    
    override val providerName: String = "Ollama (Local Server)"

    override suspend fun generateResponse(prompt: String, systemPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val networkMessages = listOf(
                NetworkMessage(role = "system", content = systemPrompt),
                NetworkMessage(role = "user", content = prompt)
            )
            val request = ChatRequest(model = targetModel, messages = networkMessages)
            val response = NetworkModule.apiService.generateChatCompletion(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message.content)
            } else {
                Result.failure(Exception("Ollama Error: ${response.code()} ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
