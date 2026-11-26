package com.perceptionparadox.oneura.data.ai

/**
 * Which job a generation is doing. xAI uses this to pick a model:
 * companion replies stay on the stronger model, and background loops
 * (inner voice, dreams, meta reflection) use the cheaper fast model.
 * Other providers ignore it and keep their single configured model.
 */
enum class GenerationPurpose {
    COMPANION,
    BACKGROUND
}
