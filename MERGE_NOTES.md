# Oneura merge notes (Nov 1, 2026)

Merged on Grok Bot's box for Jason to review. **Nothing has been pushed to GitHub.**

## Sources
| Copy | Date | Role |
|---|---|---|
| `laptop/androidstudio` (`1.0-connection-fix`, versionCode 3) | Jul 18 | **Base**. Keeps everything it had. |
| `laptop/antigravity` (`1.0`) | Jul 24 | Dream Mode ported in. |
| `github-copy` (jaydd73/Oneura) | Jul 8 | Common ancestor, used only to work out which side changed what. Nothing taken from it. |

## What each side changed (compared with the GitHub ancestor)
- **Android Studio only:** the AiProvider abstraction (`data/ai/*`: Gemini Cloud, Ollama, MediaPipe on-device), SmartConfigEngine (auto-finds the AI server), bigger Settings, Network, Chat and SettingsViewModel, a clear-chat button, `CognitiveDao.clearAllMessages`, the top app bar, newer Gradle/AGP/Kotlin and KSP for Room.
- **Antigravity only:** Dream Mode, made up of `CognitiveRepository.dreamsStream` and `triggerDreamConsolidation()`, `BrainViewModel.dreams` and `dream()` wiring, a Dreams tab in MainActivity (DreamScreen, DreamCard, DreamStageVisual, DreamVisualFallback, CognitiveMetricBar), and `dream_light.jpg`/`dream_deep.jpg`. It also has a different Gradle setup.
- **Inner-voice reflections** (`generateInnerVoiceReflection`) and `handleUserMessage` were already in the GitHub ancestor. Android Studio had already moved them onto AiProvider, so the base version was kept.
- **Entities, Room DB, theme Type/Theme are the same in all three copies.** The DAO in the base is a superset of Antigravity's. There are **no schema changes and no migration needed.**

## What was ported, and how
1. `CognitiveRepository`: added `dreamsStream` and `triggerDreamConsolidation()`. These now call the **selected AiProvider** instead of `NetworkModule.apiService`, which was Ollama-only, so dreaming works with Gemini, Ollama, or on-device. Other changes in this step:
   - It skips the call if there's nothing to consolidate.
   - JSON parsing is sturdier: it strips code fences and pulls out the outermost `{...}`.
   - `sleepStage` is checked against LIGHT/DEEP/REM.
   - It falls back to the raw text if parsing fails.
2. `BrainViewModel`: `dreams` now comes from Room instead of an empty placeholder. `dream()` launches consolidation, and a new `isDreaming` flag stops double taps.
3. `MainActivity`: the tabs are now Chat / Reflections / **Dreams** / Settings / Logs, and the Dreams composables were ported. Fixes made along the way:
   - The Antigravity code wrapped a composable `Image` call in `try/catch`, which Compose doesn't allow and won't compile. It now uses `R.drawable.*` directly.
   - `LinearProgressIndicator(progress = Float)` was changed to the lambda form, since the old one is deprecated.
   - The Consolidate button is disabled and shows "Dreaming..." while it runs.
4. The two dream images were copied into `res/drawable`. `placeholder.png` wasn't carried over because nothing references it.

## Not taken from Antigravity (on purpose)
- Its Gradle files (`build.gradle.kts`, `libs.versions.toml`, wrapper 9.1.0). They're older, they drop the Gemini SDK, and they use `annotationProcessor` for Room, which doesn't generate code for Kotlin. The base's KSP setup is the correct one.
- `BuildConfig.java`. This is a generated file and shouldn't be in source. The GitHub copy's `*_Impl.kt` Room output was also left out.
- Its Settings, Network and ChatViewModel files, which are identical to the GitHub ancestor and older than the base.

## Build fixes
- **The MediaPipe dependency was missing.** `MediaPipeLocalProvider.kt` imports `com.google.mediapipe.tasks.genai`, but the Jul 18 build files never declared it. Added `com.google.mediapipe:tasks-genai:0.10.35`.
- `gradlew` is now executable.
- The version was bumped to `1.1-merged` (versionCode 4) so it installs over 1.0-connection-fix.

## Build status: VERIFIED
- The build ran on the box with Temurin JDK 21.0.13, Android SDK platform 37.0, build-tools 37.0.0, and the project's own wrapper (Gradle 9.6.1, AGP 9.2.1, Kotlin 2.4.0).
- `./gradlew assembleDebug testDebugUnitTest` reported **BUILD SUCCESSFUL** with 0 Kotlin errors.
  - The first full run failed once in `packageDebug`, most likely from low memory on the box. A rerun passed, and so did a later clean build of every changed task.
- Warnings are all from base code that was already there: `fallbackToDestructiveMigration()` is deprecated, and MediaPipe's `LlmInference` is marked deprecated.
- The debug APK is `/workspace/oneura/oneura-merged-debug.apk`, about 142 MB because MediaPipe's native libraries are bundled for every ABI.
- **Not verified:** the app hasn't been run on a device or emulator, and no AI backend calls were tested.

## TODOs / open questions
- `BrainViewModel.reflect()` is an empty stub in all three copies, and what it was meant to do is unclear.
- DEEP dreams show "Cognitive Vectors" percentages that are **hard-coded placeholders**: 0.87, 0.94, 0.41, 0.76.
- There is no REM artwork, so REM reuses `dream_deep.jpg`, as Antigravity did.
- `GeminiCloudProvider` tries `gemini-1.5-flash`, `gemini-1.5-pro` and `gemini-1.0-pro`. These older model IDs may be retired, so check them against the current Gemini model list. This code is from the base and wasn't changed.
- The MediaPipe model path is hard-coded to `/data/user/0/com.perceptionparadox.oneura/files/model.bin`. This is from the base.
- Consider ABI splits or a `ndk.abiFilters` setting to shrink the APK.
- `local.properties` (Windows SDK path) is git-ignored and was left as-is.

## Secrets scan
No API keys, tokens, or private keys were found in `merged/`, `laptop/*`, or `github-copy/`. The Gemini key is typed in by the user at runtime and stored in DataStore. The Settings hints show a private LAN address for the Ollama server, which isn't a secret.

## Git history (local repo only, no remote)
1. Baseline: Android Studio copy
2. Port Dream Mode from Antigravity, routed through AiProvider
3. Build fixes (MediaPipe dependency, gradlew +x, version bump)
4. MERGE_NOTES.md
