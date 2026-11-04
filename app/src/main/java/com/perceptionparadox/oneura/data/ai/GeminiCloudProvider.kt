package com.perceptionparadox.oneura.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.perceptionparadox.oneura.data.remote.InternalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCloudProvider(private val apiKey: String) : AiProvider {
    
    override val providerName: String = "Gemini Cloud"

    override suspend fun generateResponse(prompt: String, systemPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(Exception("API Key is missing."))
        }

        // California is supported. If 404 occurs, model ID might be project-specific or misspelled.
        val modelsToTry = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro")
        var lastError: Exception? = null

        for (modelName in modelsToTry) {
            try {
                InternalLogger.log("SYS", "Checking Cloud Brain: $modelName")
                Log.d("OneuraAI", "Attempting model: $modelName")
                
                val model = GenerativeModel(modelName = modelName, apiKey = cleanKey)
                val fullPrompt = "$systemPrompt\n\nUser: $prompt"
                val response = model.generateContent(fullPrompt)
                
                val text = response.text
                if (!text.isNullOrBlank()) {
                    InternalLogger.log("SYS", "Success with $modelName")
                    Log.d("OneuraAI", "Success with model: $modelName")
                    return@withContext Result.success(text)
                } else {
                    Log.w("OneuraAI", "Model $modelName returned empty text")
                }
            } catch (e: Exception) {
                lastError = e
                val errorMsg = e.message ?: "Unknown"
                InternalLogger.log("ERR", "$modelName failed: ${errorMsg.take(60)}")
                Log.e("OneuraAI", "Model $modelName failed", e)
            }
        }

        val finalMsg = lastError?.message ?: "Unknown Error"
        val userFriendlyError = when {
            finalMsg.contains("403") -> "Access Denied: Check if Generative Language API is enabled in Google AI Studio."
            finalMsg.contains("404") -> "Not Found: Your key doesn't have access to these models. Try creating a fresh key in a NEW project."
            finalMsg.contains("429") -> "Slow down! (Quota reached)."
            else -> "Cloud Brain Error: ${lastError?.localizedMessage}"
        }
        Result.failure(Exception(userFriendlyError))
    }
}
