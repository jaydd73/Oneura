package com.perceptionparadox.oneura.data.ai

interface AiProvider {
    suspend fun generateResponse(prompt: String, systemPrompt: String): Result<String>
    val providerName: String
}
