package com.perceptionparadox.oneura.data.repository

import android.util.Log
import com.perceptionparadox.oneura.data.local.dao.CognitiveDao
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import com.perceptionparadox.oneura.data.remote.InternalLogger
import com.perceptionparadox.oneura.data.remote.NetworkModule
import com.perceptionparadox.oneura.data.remote.model.ChatRequest
import com.perceptionparadox.oneura.data.remote.model.NetworkMessage
import kotlinx.coroutines.flow.Flow

class CognitiveRepository(
    private val dao: CognitiveDao,
    private val targetModelProvider: () -> String
) {
    val outerVoiceChatStream: Flow<List<ChatMessageEntity>> = dao.getAllMessagesFlow()
    val innerVoiceReflectionStream: Flow<List<ReflectionEntity>> = dao.getAllReflectionsFlow()

    suspend fun handleUserMessage(userText: String, metaStrategyPrompt: String) {
        InternalLogger.log("SYS", "Processing message: $userText")
        
        dao.insertMessage(ChatMessageEntity(sender = "USER", content = userText, isOuterVoice = true))

        val networkMessages = listOf(
            NetworkMessage(role = "system", content = metaStrategyPrompt),
            NetworkMessage(role = "user", content = userText)
        )
        
        val request = ChatRequest(model = targetModelProvider(), messages = networkMessages)
        
        try {
            // ALWAYS use the latest apiService from the module
            val response = NetworkModule.apiService.generateChatCompletion(request)
            
            if (response.isSuccessful && response.body() != null) {
                val aiResponseText = response.body()!!.message.content
                InternalLogger.log("SYS", "AI Replied: ${aiResponseText.take(20)}...")

                dao.insertMessage(ChatMessageEntity(sender = "AI", content = aiResponseText, isOuterVoice = true))
                
                generateInnerVoiceReflection(aiResponseText)
            } else {
                val errorMsg = "API Error: ${response.code()} ${response.errorBody()?.string()}"
                InternalLogger.log("ERR", errorMsg)
                dao.insertMessage(ChatMessageEntity(sender = "AI", content = errorMsg, isOuterVoice = true))
            }
        } catch (e: Exception) {
            InternalLogger.log("ERR", "Network Failure: ${e.message}")
            dao.insertMessage(ChatMessageEntity(sender = "AI", content = "Connection Error: ${e.message}", isOuterVoice = true))
        }
    }

    private suspend fun generateInnerVoiceReflection(lastAiResponse: String) {
        val reflectionPrompt = "Analyze your previous response: '$lastAiResponse'. What was your hidden motivation?"
        val request = ChatRequest(
            model = targetModelProvider(),
            messages = listOf(NetworkMessage(role = "system", content = reflectionPrompt))
        )
        
        try {
            val response = NetworkModule.apiService.generateChatCompletion(request)
            if (response.isSuccessful && response.body() != null) {
                val reflectionText = response.body()!!.message.content
                dao.insertReflection(ReflectionEntity(content = reflectionText, triggerSource = "AUTO_MONOLOGUE"))
            }
        } catch (e: Exception) {
            InternalLogger.log("ERR", "Reflection Failed: ${e.message}")
        }
    }
}
