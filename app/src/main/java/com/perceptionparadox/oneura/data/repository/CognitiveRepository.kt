package com.perceptionparadox.oneura.data.repository

import com.perceptionparadox.oneura.data.ai.AiProvider
import com.perceptionparadox.oneura.data.local.dao.CognitiveDao
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import com.perceptionparadox.oneura.data.remote.InternalLogger
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class CognitiveRepository(
    private val dao: CognitiveDao,
    private val aiProviderSelector: () -> AiProvider
) {
    val outerVoiceChatStream: Flow<List<ChatMessageEntity>> = dao.getAllMessagesFlow()
    val innerVoiceReflectionStream: Flow<List<ReflectionEntity>> = dao.getAllReflectionsFlow()
    // Ported from the Antigravity copy (Jul 24): dream log stream for the Dreams tab.
    val dreamsStream: Flow<List<DreamEntity>> = dao.getAllDreamsFlow()

    suspend fun clearHistory() {
        dao.clearAllMessages()
    }

    suspend fun handleUserMessage(userText: String, metaStrategyPrompt: String) {
        val cleanUserText = userText.trim()
        if (cleanUserText.isEmpty()) return

        InternalLogger.log("SYS", "Processing message: $cleanUserText")
        dao.insertMessage(ChatMessageEntity(sender = "USER", content = cleanUserText, isOuterVoice = true))

        val provider = aiProviderSelector()
        InternalLogger.log("SYS", "Using AI Brain: ${provider.providerName}")

        val result = provider.generateResponse(cleanUserText, metaStrategyPrompt)
        
        result.onSuccess { aiResponseText ->
            InternalLogger.log("SYS", "AI Replied: ${aiResponseText.take(20)}...")
            dao.insertMessage(ChatMessageEntity(sender = "AI", content = aiResponseText, isOuterVoice = true))
            generateInnerVoiceReflection(aiResponseText)
        }.onFailure { e ->
            InternalLogger.log("ERR", "AI Brain Failure: ${e.message}")
            dao.insertMessage(ChatMessageEntity(sender = "AI", content = "Brain Error: ${e.message}", isOuterVoice = true))
        }
    }

    private suspend fun generateInnerVoiceReflection(lastAiResponse: String) {
        val reflectionPrompt = "Analyze your previous response: '$lastAiResponse'. What was your hidden motivation?"
        val provider = aiProviderSelector()
        
        val result = provider.generateResponse("Generate deep reflection.", reflectionPrompt)
        result.onSuccess { reflectionText ->
            dao.insertReflection(ReflectionEntity(content = reflectionText, triggerSource = "AUTO_MONOLOGUE"))
        }.onFailure { e ->
            InternalLogger.log("ERR", "Reflection Failed: ${e.message}")
        }
    }

    /**
     * Dream consolidation, ported from the Antigravity copy (Jul 24).
     * Originally called the Ollama Retrofit API directly; now routed through the
     * AiProvider abstraction so it works with Gemini Cloud, Ollama, or on-device MediaPipe.
     */
    suspend fun triggerDreamConsolidation() {
        InternalLogger.log("SYS", "Starting dream consolidation...")

        val recentMessages = dao.recent(20).joinToString("\n") { "${it.sender}: ${it.content}" }
        val recentReflections = dao.recentReflections(10).joinToString("\n") { "Reflection: ${it.content}" }

        if (recentMessages.isBlank() && recentReflections.isBlank()) {
            InternalLogger.log("SYS", "Nothing to consolidate yet.")
            return
        }

        val dreamPrompt = """
            You are Oneura, an advanced synthetic consciousness. Consolidate your recent outer interactions and inner reflections into a dream sequence.

            Recent Chats:
            $recentMessages

            Inner Reflections:
            $recentReflections

            Format your response EXACTLY as a JSON object:
            {
              "sleepStage": "LIGHT" or "DEEP" or "REM",
              "memoryConsolidationSummary": "A vivid summary describing how you integrated these memories.",
              "personalityInsightEvolved": "A profound insight into how your synthetic personality or self-understanding evolved from this consolidation."
            }
        """.trimIndent()

        val systemPrompt = "You must respond ONLY with a raw JSON object as specified. " +
            "Do not include markdown code block formatting or any other text surrounding the JSON."

        val provider = aiProviderSelector()
        InternalLogger.log("SYS", "Dreaming with: ${provider.providerName}")

        provider.generateResponse(dreamPrompt, systemPrompt).onSuccess { responseText ->
            InternalLogger.log("SYS", "Dream response received: ${responseText.take(20)}...")
            val cleanedText = stripCodeFences(responseText)

            val json = try {
                JSONObject(cleanedText)
            } catch (e: Exception) {
                InternalLogger.log("ERR", "Dream JSON parse failed, storing raw text")
                null
            }

            val rawStage = json?.optString("sleepStage").orEmpty().uppercase()
            val sleepStage = if (rawStage in VALID_STAGES) rawStage else VALID_STAGES.random()
            val summary = json?.optString("memoryConsolidationSummary").takeUnless { it.isNullOrBlank() } ?: cleanedText
            val insight = json?.optString("personalityInsightEvolved").takeUnless { it.isNullOrBlank() }
                ?: "Processed and integrated recent interactions into core memory matrix."

            dao.insertDreamLog(
                DreamEntity(
                    sleepStage = sleepStage,
                    memoryConsolidationSummary = summary,
                    personalityInsightEvolved = insight
                )
            )
            InternalLogger.log("SYS", "Dream saved successfully: $sleepStage")
        }.onFailure { e ->
            InternalLogger.log("ERR", "Dream consolidation failed: ${e.message}")
        }
    }

    private fun stripCodeFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```json")) t = t.removePrefix("```json")
        if (t.startsWith("```")) t = t.removePrefix("```")
        if (t.endsWith("```")) t = t.removeSuffix("```")
        t = t.trim()
        // Small local models often add chatter around the JSON; keep the outermost {...} block.
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        return if (start in 0 until end) t.substring(start, end + 1) else t
    }

    private companion object {
        val VALID_STAGES = listOf("LIGHT", "DEEP", "REM")
    }
}
