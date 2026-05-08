# Airbot Module Design

Date: 2026-05-08

## Goal

Create a new `:airbot` Android library module that owns the robot interaction experience. The module will make the feature boundaries explicit: character rendering, conversation UI and orchestration, voice input UI, experience-level audio adaptation, and dialogue state.

The existing `:audio` module remains the low-level audio engine. It continues to own recording, playback, KWS, Opus, native C++ integration, and audio assets.

## Current Problem

The current `:character` module contains more than character rendering. It also owns conversation UI, voice input UI, dialogue state, and `ConversationViewModel`. It depends on `:audio`, `:core`, and `:services`, while `:app` also directly depends on and assembles all of those modules.

This makes the module name misleading and pushes robot-experience composition into `:app`. The desired structure is a clear `:airbot` feature module that composes the robot-facing experience and leaves `:app` as a shell.

## Chosen Approach

Use a feature-module merge:

- Add `:airbot` with namespace `com.airobot.airbot`.
- Move current `:character` source into `:airbot`, organized by responsibility.
- Keep `:audio` as an independent low-level library.
- Update `:app` to consume `:airbot` APIs and stop importing `com.airobot.character.*`.
- Remove or disable `:character` after the migration is complete.
- Update `.agent/doc` architecture documentation to describe the new module boundaries.

## Module Boundaries

```text
app -> airbot
airbot -> audio, core, framework, services
audio -> no airbot dependency
framework -> no airbot dependency
services -> no airbot dependency
core -> no airbot dependency
```

`airbot` is allowed to coordinate `AudioService`, `NetCommService`, `RobotStateEngine`, and services state because it is the robot-experience feature boundary.

`audio` stays reusable and UI-free. It does not know about robot states, cards, Compose, or conversation flow.

## Airbot Package Structure

```text
airbot/src/main/kotlin/com/airobot/airbot/
  character/
    DynamicEyes.kt
    RobotCharacter.kt
  conversation/
    ConversationViewModel.kt
    DialogueBubble.kt
    TypewriterText.kt
    UserMessageBubble.kt
  voice/
    RobotVoiceInputPanel.kt
    VoiceWaveform.kt
  audio/
    AirbotAudioController.kt
  state/
    Message.kt
    RobotEngineState.kt
    RobotStateEngine.kt
    RobotUiState.kt
```

The initial migration may keep the current class names to reduce risk. Follow-up cleanup can split state files further if needed.

## Public API

The first migration keeps public APIs close to the current behavior:

- `RobotCharacter` renders the character.
- `RobotVoiceInputPanel` renders voice input controls.
- `DialogueBubble` renders the assistant response bubble.
- `ConversationViewModel` owns speech-driven dialogue flow.
- `RobotStateEngine` owns the shared robot engine state.
- `RobotUiState` remains the screen-level visual state model.

The package names change from `com.airobot.character.*` to `com.airobot.airbot.*`.

## App Integration

`AiRobotMainScreen` remains in `:app` during the first migration, but all imports are updated to `com.airobot.airbot.*`. This keeps the first change focused on module extraction and dependency cleanup.

A later change may move the full robot main screen composition into `:airbot` if the app shell should become thinner.

## Audio Integration

`airbot.audio` is an experience-level adapter package. It may contain small coordination classes around `AudioService`, but it must not duplicate recorder, player, codec, KWS, CMake, or native logic.

The low-level `AudioService`, `AudioEvent`, and `AudioConfig` remain in `:audio`.

## Documentation Updates

Update `.agent/doc/architecture.md` to:

- Replace `character/` with `airbot/` as the robot interaction feature module.
- Describe the internal `airbot` package layout.
- Keep `audio/` documented as a low-level audio processing module.
- Update dependency direction notes so `app` consumes `airbot`, and `airbot` composes `audio`, `core`, `framework`, and `services`.

Update `.agent/rules/airobot-rules.md` if needed to reflect the new module responsibility rule:

- `airbot` owns robot interaction composition.
- `audio` remains a reusable audio engine.
- `framework` remains stateless and feature-independent.

## Testing And Verification

Minimum verification:

- Gradle sync-level module inclusion is valid.
- `:airbot` compiles.
- `:app` compiles after import and dependency updates.
- No source import remains for `com.airobot.character`.
- `.agent/doc` reflects the new architecture.

Preferred command:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

If native audio build changes are avoided, this should primarily validate Kotlin, Compose, Hilt, and module dependencies.

## Out Of Scope

- Rewriting low-level audio implementation.
- Moving C++ or audio model assets from `:audio`.
- Redesigning the main robot screen layout.
- Changing runtime dialogue behavior beyond package/module migration.
- Adding new robot states or services.
