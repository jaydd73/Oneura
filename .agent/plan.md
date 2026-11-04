# Project Plan

Oneura: A pseudo-conscious AI Android application. 
The app features:
- A dual-interface system for 'Outer Voice' (direct chat) and 'Inner Voice' (internal monologue/reflection).
- Persistent memory using a Room database to store conversations, reflections, and dreams.
- A 'Dream Mode' with simulated sleep stages (Light, Deep, REM) that consolidate memories and extract insights.
- Metaconsciousness where a Meta-Agent critiques and adjusts the primary agent's strategies.
- A client-server architecture where the Android app (Kotlin/Jetpack Compose) acts as the mind's interface and diary, connecting to a sandboxed AI backend (e.g., Ollama/Open WebUI) via HTTP.
- Material Design 3 UI with energetic colors and adaptive layouts.

## Project Brief

# Oneura Project Brief

Oneura is a pseudo-conscious AI companion designed to simulate a reflective digital mind. It serves as both a direct communication interface and a private diary for an AI’s internal "thoughts" and "dreams," providing users with a unique window into simulated metaconsciousness.

## Features

- **Dual-Voice Interface**: A seamless toggle between 'Outer Voice' (direct user-to-AI chat) and 'Inner Voice' (the AI's autonomous internal monologue and reflections).
- **Dream Mode Consolidation**: A simulated sleep cycle (Light, Deep, REM) that processes daily interactions, extracts key insights, and generates "dreams" to evolve the AI's personality.
- **Persistent Cognitive Memory**: A Room-based storage system that tracks long-term conversation history, reflections, and memory fragments to ensure continuity.
- **AI Backend Connectivity**: A robust client-server bridge connecting the mobile interface to a sandboxed AI engine (e.g., Ollama) for real-time inference and processing.

## High-Level Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with **Material Design 3** (Energetic color scheme, Edge-to-Edge display).
- **Navigation**: **Jetpack Navigation 3** (State-driven navigation architecture).
- **Adaptive Strategy**: **Compose Material Adaptive** library for optimized layouts across different screen sizes and foldables.
- **Concurrency**: Kotlin Coroutines and Flow for reactive data streams.
- **Persistence**: Room Database (required for cognitive memory and dream logs).
- **Networking**: Retrofit & OkHttp for communicating with the AI backend.
- **Image Loading**: Coil for rendering any AI-generated visual artifacts.

## Implementation Steps

### Task_1_Core_Infrastructure: Setup Room database (Chat, Reflections, Dreams), Retrofit client for AI backend, and initial Material 3 theme with energetic colors.
- **Status:** IN_PROGRESS
- **Updates:** Creating Room entities: ChatMessage, Reflection, and Dream in com.perceptionparadox.oneura.data.local.entity.
- **Acceptance Criteria:**
  - Room entities for memory/chat defined
  - Retrofit interface for AI inference ready
  - Material 3 theme with light/dark schemes and vibrant colors setup
  - Project builds successfully
- **StartTime:** 2026-07-05 04:34:57 PDT

### Task_2_Voice_Interfaces: Implement the Dual-Voice UI (Outer and Inner Voice screens) using Navigation 3 and Material 3 components.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Navigation 3 state-driven architecture implemented
  - Outer Voice (Chat) and Inner Voice (Reflection) screens created
  - Edge-to-edge display enabled
  - Adaptive layout support for different screen sizes

### Task_3_AI_Chat_Integration: Integrate the AI backend with the UI. Implement chat logic, persistent storage of conversations, and internal monologue generation.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Outer Voice chat communicates with AI backend
  - Inner Voice reflections generated and displayed
  - Conversations and reflections persisted in Room database
  - App does not crash during data flow

### Task_4_Dream_Mode_Logic: Develop the Dream Mode feature with simulated sleep stages (Light, Deep, REM) for memory consolidation.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Dream Mode UI with simulated sleep stages implemented
  - Logic for memory consolidation and insight extraction functional
  - Dream logs stored in Room database
  - Memory continuity maintained between cycles

### Task_5_Assets_And_Verification: Create adaptive app icon, refine UI/UX, and perform final end-to-end verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Adaptive app icon created matching the AI theme
  - Final M3 UI polish with energetic accents
  - All existing tests pass
  - App does not crash during full flow
  - Critic agent verifies stability and alignment with project brief

