package com.perceptionparadox.oneura.data.ai

/**
 * Model ids checked against xAI's public model list (docs.x.ai, Nov 2026).
 *
 * Companion default is `grok-4.6`. The older fast slug `grok-4-1-fast` was
 * retired on 15 May 2026 and now redirects to `grok-4.3`, which is the
 * current cheaper model ($1.25 / $2.50 per 1M tokens, default reasoning
 * effort "low"). Both ids are settings, so a later model can be typed in
 * without a code change. `grok-beta`, `grok-2`, `grok-3`, and `grok-latest`
 * are intentionally not defaults.
 */
object XaiModels {
    const val CHAT_COMPLETIONS_URL = "https://api.x.ai/v1/chat/completions"
    const val DEFAULT_COMPANION = "grok-4.6"
    const val DEFAULT_BACKGROUND = "grok-4.3"

    val discouragedIds: Set<String> = setOf(
        "grok-beta",
        "grok-2",
        "grok-2-latest",
        "grok-3",
        "grok-3-mini",
        "grok-latest",
        "grok-4-1-fast",
        "grok-4-1-fast-reasoning",
        "grok-4-1-fast-non-reasoning"
    )

    fun resolveModel(purpose: GenerationPurpose, companion: String, background: String): String {
        val chosen = when (purpose) {
            GenerationPurpose.COMPANION -> companion
            GenerationPurpose.BACKGROUND -> background
        }.trim()
        if (chosen.isNotEmpty()) return chosen
        return when (purpose) {
            GenerationPurpose.COMPANION -> DEFAULT_COMPANION
            GenerationPurpose.BACKGROUND -> DEFAULT_BACKGROUND
        }
    }

    fun isDiscouraged(model: String): Boolean = model.trim().lowercase() in discouragedIds
}
