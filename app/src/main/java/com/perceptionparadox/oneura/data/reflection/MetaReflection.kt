package com.perceptionparadox.oneura.data.reflection

import com.google.gson.JsonObject
import com.google.gson.JsonParser

enum class MetaReflectionKind {
    OBSERVATION,
    CRITIQUE,
    STRATEGY_SHIFT
}

data class ParsedMetaReflection(
    val kind: MetaReflectionKind,
    val note: String,
    val strategy: String
)

data class TaggedReflection(
    val triggerSource: String,
    val content: String
)

/**
 * Meta-agent reflection: one stored entry tagged as an observation, a critique,
 * or a strategy shift. The latest of each feeds a short block in later prompts.
 * Parsing is pure JVM so it can be unit tested without Room or Android.
 */
object MetaReflection {
    const val EVERY_N_TURNS = 4
    const val ORIGIN_AUTO = "META_AUTO"
    const val ORIGIN_MANUAL = "META_MANUAL"

    const val SYSTEM_PROMPT =
        "You are Oneura's meta-agent. Critique the companion's recent strategy and conversation. " +
            "Respond with ONLY a raw JSON object, no markdown, of the form " +
            "{\"kind\":\"OBSERVATION\",\"note\":\"...\",\"strategy\":\"\"}. " +
            "kind must be OBSERVATION, CRITIQUE, or STRATEGY_SHIFT. " +
            "Use OBSERVATION to name a pattern, CRITIQUE when the current approach is weak, " +
            "and STRATEGY_SHIFT when the companion should change how it behaves. " +
            "strategy is one or two sentences of instruction. It is required for STRATEGY_SHIFT " +
            "and may be an empty string otherwise."

    fun userPrompt(transcript: String, priorReflections: String): String = """
        Recent conversation:
        $transcript

        Prior meta reflections:
        $priorReflections

        Write the next meta reflection as JSON.
    """.trimIndent()

    fun triggerSource(origin: String, kind: MetaReflectionKind): String = "$origin:${kind.name}"

    fun kindOf(triggerSource: String): MetaReflectionKind? {
        if (!triggerSource.startsWith("META_")) return null
        val name = triggerSource.substringAfter(':', missingDelimiterValue = "")
        return MetaReflectionKind.entries.find { it.name == name }
    }

    fun isMeta(triggerSource: String): Boolean = kindOf(triggerSource) != null

    fun displayLabel(triggerSource: String): String = when (kindOf(triggerSource)) {
        MetaReflectionKind.OBSERVATION -> "Observation"
        MetaReflectionKind.CRITIQUE -> "Critique"
        MetaReflectionKind.STRATEGY_SHIFT -> "Strategy shift"
        null -> if (triggerSource == "AUTO_MONOLOGUE") "Inner voice" else "Reflection"
    }

    /**
     * Reads a model reply. Valid JSON wins. Prose with no JSON object is kept
     * as a critique so a chatty model still leaves a memory. Empty or broken
     * JSON with nothing else to keep returns null.
     */
    fun parse(raw: String): ParsedMetaReflection? {
        val jsonText = extractJsonObject(raw)
        if (jsonText != null) {
            parseJson(jsonText)?.let { return it }
        }
        val prose = stripFences(raw).trim()
        if (prose.isEmpty() || prose.contains('{')) return null
        return ParsedMetaReflection(MetaReflectionKind.CRITIQUE, prose, "")
    }

    fun storedContent(parsed: ParsedMetaReflection): String? {
        val note = parsed.note.trim()
        val strategy = parsed.strategy.trim()
        val text = when (parsed.kind) {
            MetaReflectionKind.STRATEGY_SHIFT -> when {
                note.isNotEmpty() && strategy.isNotEmpty() -> "$note\n\nStrategy: $strategy"
                strategy.isNotEmpty() -> strategy
                else -> note
            }
            else -> note.ifEmpty { strategy }
        }.trim()
        return text.ifEmpty { null }
    }

    fun strategyLine(content: String): String {
        val marker = "Strategy:"
        val index = content.lastIndexOf(marker)
        return if (index >= 0) content.substring(index + marker.length).trim() else content.trim()
    }

    /**
     * Short block for the companion system prompt. [entriesNewestFirst] must be
     * newest first; the latest strategy, critique, and observation are kept.
     */
    fun guidanceAddendum(entriesNewestFirst: List<TaggedReflection>): String {
        val metas = entriesNewestFirst.mapNotNull { entry ->
            val kind = kindOf(entry.triggerSource) ?: return@mapNotNull null
            val content = entry.content.trim()
            if (content.isEmpty()) null else kind to content
        }
        if (metas.isEmpty()) return ""

        val lines = mutableListOf<String>()
        metas.firstOrNull { it.first == MetaReflectionKind.STRATEGY_SHIFT }?.let {
            lines += "Strategy: ${excerpt(strategyLine(it.second))}"
        }
        metas.firstOrNull { it.first == MetaReflectionKind.CRITIQUE }?.let {
            lines += "Critique: ${excerpt(it.second)}"
        }
        metas.firstOrNull { it.first == MetaReflectionKind.OBSERVATION }?.let {
            lines += "Observation: ${excerpt(it.second)}"
        }
        if (lines.isEmpty()) return ""
        return "Recent self-reflections. Let the latest strategy shape how you respond.\n" +
            lines.joinToString("\n")
    }

    fun extractJsonObject(text: String): String? {
        val stripped = stripFences(text)
        val start = stripped.indexOf('{')
        val end = stripped.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return stripped.substring(start, end + 1)
    }

    fun stripFences(text: String): String {
        var trimmed = text.trim()
        if (trimmed.startsWith("```")) {
            val firstNewline = trimmed.indexOf('\n')
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1)
            if (trimmed.trimEnd().endsWith("```")) {
                trimmed = trimmed.trimEnd().removeSuffix("```")
            }
        }
        return trimmed.trim()
    }

    private fun excerpt(text: String, max: Int = 280): String {
        val clean = text.replace('\n', ' ').trim()
        return if (clean.length <= max) clean else clean.take(max - 1).trimEnd() + "…"
    }

    private fun parseJson(jsonText: String): ParsedMetaReflection? {
        val obj = try {
            val parsed = JsonParser.parseString(jsonText)
            if (!parsed.isJsonObject) return null
            parsed.asJsonObject
        } catch (e: Exception) {
            return null
        }
        val kindRaw = stringField(obj, "kind", "type", "tag")
        val kind = if (kindRaw.isNullOrBlank()) {
            MetaReflectionKind.OBSERVATION
        } else {
            mapKind(kindRaw) ?: MetaReflectionKind.CRITIQUE
        }
        val note = stringField(obj, "note", "content", "reflection", "critique", "observation").orEmpty()
        val strategy = stringField(obj, "strategy", "directive", "instruction").orEmpty()
        if (note.isBlank() && strategy.isBlank()) return null
        return ParsedMetaReflection(kind, note.trim(), strategy.trim())
    }

    private fun stringField(obj: JsonObject, vararg names: String): String? {
        for (name in names) {
            val element = obj.get(name) ?: continue
            if (element.isJsonNull || !element.isJsonPrimitive || !element.asJsonPrimitive.isString) continue
            return element.asString
        }
        return null
    }

    private fun mapKind(raw: String): MetaReflectionKind? {
        val norm = raw.trim().lowercase().replace('-', '_').replace(' ', '_')
        return when (norm) {
            "observation", "observe", "observing" -> MetaReflectionKind.OBSERVATION
            "critique", "criticism", "critic" -> MetaReflectionKind.CRITIQUE
            "strategy_shift", "strategy", "shift", "strategyshift" -> MetaReflectionKind.STRATEGY_SHIFT
            else -> null
        }
    }
}
