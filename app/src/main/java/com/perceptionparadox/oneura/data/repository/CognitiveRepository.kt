package com.perceptionparadox.oneura.data.repository

import com.perceptionparadox.oneura.data.ai.AiProvider
import com.perceptionparadox.oneura.data.ai.GenerationPurpose
import com.perceptionparadox.oneura.data.local.dao.CognitiveDao
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.local.entity.ReflectionEntity
import com.perceptionparadox.oneura.data.reflection.MetaReflection
import com.perceptionparadox.oneura.data.reflection.TaggedReflection
import com.perceptionparadox.oneura.data.remote.InternalLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject

class CognitiveRepository(
    private val dao: CognitiveDao,
    private val aiProviderSelector: () -> AiProvider
) {
    private val metaLock = Mutex()
    private var lastAutoReflectedTurn = -1
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

        val result = provider.generateResponse(
            cleanUserText,
            composeSystemPrompt(metaStrategyPrompt),
            GenerationPurpose.COMPANION
        )
        
        result.onSuccess { aiResponseText ->
            InternalLogger.log("SYS", "AI Replied: ${aiResponseText.take(20)}...")
            dao.insertMessage(ChatMessageEntity(sender = "AI", content = aiResponseText, isOuterVoice = true))
            generateInnerVoiceReflection(aiResponseText)
            maybeAutoMetaReflection()
        }.onFailure { e ->
            InternalLogger.log("ERR", "AI Brain Failure: ${e.message}")
            dao.insertMessage(ChatMessageEntity(sender = "AI", content = "Brain Error: ${e.message}", isOuterVoice = true))
        }
    }

    /**
     * Manual meta reflection from the Inner Voice screen.
     * Returns the stored text, or a failure the UI can show.
     */
    suspend fun runMetaReflection(): Result<String> {
        if (!metaLock.tryLock()) {
            return Result.failure(IllegalStateException("A reflection is already running."))
        }
        return try {
            reflectLocked(MetaReflection.ORIGIN_MANUAL)
        } finally {
            metaLock.unlock()
        }
    }

    private suspend fun generateInnerVoiceReflection(lastAiResponse: String) {
        val reflectionPrompt = "Analyze your previous response: '$lastAiResponse'. What was your hidden motivation?"
        val provider = aiProviderSelector()
        
        val result = provider.generateResponse(
            "Generate deep reflection.",
            composeSystemPrompt(reflectionPrompt),
            GenerationPurpose.BACKGROUND
        )
        result.onSuccess { reflectionText ->
            dao.insertReflection(ReflectionEntity(content = reflectionText, triggerSource = "AUTO_MONOLOGUE"))
        }.onFailure { e ->
            InternalLogger.log("ERR", "Reflection Failed: ${e.message}")
        }
    }

    /**
     * Dream consolidation, ported from the Antigravity copy (Jul 24).
     * Originally called the Ollama Retrofit API directly; now routed through the
     * AiProvider abstraction so it works with Gemini Cloud, xAI, Ollama, or on-device MediaPipe.
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

        provider.generateResponse(
            dreamPrompt,
            composeSystemPrompt(systemPrompt),
            GenerationPurpose.BACKGROUND
        ).onSuccess { responseText ->
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

    private suspend fun composeSystemPrompt(base: String): String {
        val addendum = MetaReflection.guidanceAddendum(
            dao.recentReflections(12).map { TaggedReflection(it.triggerSource, it.content) }
        )
        return if (addendum.isBlank()) base else "$base\n\n$addendum"
    }

    private suspend fun maybeAutoMetaReflection() {
        val turns = dao.countUserMessages()
        if (turns < MetaReflection.EVERY_N_TURNS || turns % MetaReflection.EVERY_N_TURNS != 0) return
        if (turns == lastAutoReflectedTurn) return
        if (!metaLock.tryLock()) return
        try {
            if (turns == lastAutoReflectedTurn) return
            lastAutoReflectedTurn = turns
            reflectLocked(MetaReflection.ORIGIN_AUTO).onFailure { error ->
                InternalLogger.log("ERR", "Meta reflection failed: ${error.message}")
            }
        } finally {
            metaLock.unlock()
        }
    }

    private suspend fun reflectLocked(origin: String): Result<String> {
        val recentMessages = dao.recent(12)
        if (recentMessages.isEmpty()) {
            return Result.failure(
                IllegalStateException("Nothing to reflect on yet. Talk with Oneura first.")
            )
        }
        val transcript = recentMessages.asReversed().joinToString("\n") { message ->
            val who = if (message.sender == "USER") "User" else "Oneura"
            "$who: ${message.content.take(500)}"
        }.take(6000)

        val prior = dao.recentReflections(20)
            .filter { MetaReflection.isMeta(it.triggerSource) }
            .take(5)
            .joinToString("\n") { entry ->
                "${MetaReflection.displayLabel(entry.triggerSource)}: ${entry.content.take(400)}"
            }
            .ifBlank { "None yet." }

        val provider = aiProviderSelector()
        InternalLogger.log("SYS", "Meta reflection with ${provider.providerName}")
        val generated = provider.generateResponse(
            MetaReflection.userPrompt(transcript, prior),
            MetaReflection.SYSTEM_PROMPT,
            GenerationPurpose.BACKGROUND
        )
        val text = generated.getOrElse { error ->
            return Result.failure(IllegalStateException(error.message ?: "Reflection failed."))
        }
        val parsed = MetaReflection.parse(text)
            ?: return Result.failure(IllegalStateException("The reflection could not be read. Try again."))
        val content = MetaReflection.storedContent(parsed)
            ?: return Result.failure(IllegalStateException("The reflection was empty."))

        dao.insertReflection(
            ReflectionEntity(
                content = content,
                triggerSource = MetaReflection.triggerSource(origin, parsed.kind)
            )
        )
        InternalLogger.log("SYS", "Meta reflection saved: ${parsed.kind.name}")
        return Result.success(content)
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
