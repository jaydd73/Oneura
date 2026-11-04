package com.perceptionparadox.oneura.data.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaPipeLocalProvider(private val context: Context, private val modelPath: String) : AiProvider {
    
    override val providerName: String = "On-Device (Private)"

    private var llmInference: LlmInference? = null

    init {
        if (File(modelPath).exists()) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    override suspend fun generateResponse(prompt: String, systemPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference ?: return@withContext Result.failure(Exception("Local Brain not downloaded yet."))
        
        try {
            val fullPrompt = "$systemPrompt\n\nUser: $prompt\nAssistant: "
            val result = inference.generateResponse(fullPrompt)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
