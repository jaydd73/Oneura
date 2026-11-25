package com.perceptionparadox.oneura.data.ai

interface AiProvider {
    val providerName: String

    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        purpose: GenerationPurpose
    ): Result<String>

    suspend fun generateResponse(prompt: String, systemPrompt: String): Result<String> =
        generateResponse(prompt, systemPrompt, GenerationPurpose.COMPANION)
}
