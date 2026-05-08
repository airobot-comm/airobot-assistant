# Airbot Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the misleading `:character` feature module with a new `:airbot` module that clearly groups character, conversation, voice, experience audio, and dialogue state responsibilities.

**Architecture:** Keep `:audio` as the low-level reusable audio engine. Create `:airbot` as the robot interaction feature module, migrate current `:character` source into responsibility-based packages, and make `:app` consume `:airbot` instead of `:character`.

**Tech Stack:** Android Gradle multi-module project, Kotlin 2.x, Jetpack Compose, Hilt, Coroutines/Flow, Android native audio via existing `:audio`.

---

## File Structure

Create:

- `airbot/build.gradle.kts`: Android library Gradle module copied from `character/build.gradle.kts`, with namespace changed to `com.airobot.airbot`.
- `airbot/src/main/kotlin/com/airobot/airbot/character/DynamicEyes.kt`: migrated role/eyes component.
- `airbot/src/main/kotlin/com/airobot/airbot/character/RobotCharacter.kt`: migrated robot character component.
- `airbot/src/main/kotlin/com/airobot/airbot/conversation/ConversationViewModel.kt`: migrated voice conversation orchestration ViewModel.
- `airbot/src/main/kotlin/com/airobot/airbot/conversation/DialogueBubble.kt`: migrated assistant dialogue bubble.
- `airbot/src/main/kotlin/com/airobot/airbot/conversation/TypewriterText.kt`: migrated typewriter text component.
- `airbot/src/main/kotlin/com/airobot/airbot/conversation/UserMessageBubble.kt`: migrated user dialogue bubble.
- `airbot/src/main/kotlin/com/airobot/airbot/voice/RobotVoiceInputPanel.kt`: migrated voice input panel.
- `airbot/src/main/kotlin/com/airobot/airbot/voice/VoiceWaveform.kt`: migrated waveform component.
- `airbot/src/main/kotlin/com/airobot/airbot/audio/AirbotAudioController.kt`: small package marker and future adapter boundary for experience-level audio coordination.
- `airbot/src/main/kotlin/com/airobot/airbot/state/Message.kt`: migrated message model.
- `airbot/src/main/kotlin/com/airobot/airbot/state/RobotStateEngine.kt`: migrated engine state and state engine.
- `airbot/src/main/kotlin/com/airobot/airbot/state/RobotUiState.kt`: migrated UI state model.

Modify:

- `settings.gradle.kts`: include `:airbot` and remove `:character`.
- `app/build.gradle.kts`: replace `implementation(project(":character"))` with `implementation(project(":airbot"))`.
- `app/src/main/kotlin/com/airobot/tablet/airobotui/AiRobotMainScreen.kt`: update imports from `com.airobot.character.*` to `com.airobot.airbot.*`.
- `app/src/main/kotlin/com/airobot/tablet/airobotui/viewmodel/MainShellViewModel.kt`: update imports from `com.airobot.character.state.*` to `com.airobot.airbot.state.*`.
- `.agent/doc/architecture.md`: document `airbot` as the robot interaction module and keep `audio` as the low-level audio engine.
- `.agent/rules/airobot-rules.md`: update module responsibility rules for `airbot`, `audio`, and `framework`.

Delete after successful migration:

- `character/build.gradle.kts`
- `character/src/main/kotlin/com/airobot/character/**`

Do not modify:

- `audio/src/main/cpp/**`
- `audio/src/main/assets/**`
- `audio/src/main/kotlin/com/airobot/audio/**`, except only if a compile error proves an import update is required.
- Existing unrelated Gradle wrapper or version catalog changes already present in the worktree.

---

### Task 1: Add The Airbot Module Shell

**Files:**

- Create: `airbot/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create `airbot/build.gradle.kts`**

Use this content:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.airobot.airbot"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":framework"))
    implementation(project(":core"))
    implementation(project(":services"))
    implementation(project(":audio"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.icons.extended)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 2: Update `settings.gradle.kts`**

Replace this line:

```kotlin
include(":character")
```

with:

```kotlin
include(":airbot")
```

- [ ] **Step 3: Run module configuration verification**

Run:

```powershell
.\gradlew.bat :airbot:tasks --all
```

Expected: Gradle recognizes `:airbot` and lists tasks for the module. A dependency-resolution or compile failure is acceptable at this point only if it points to missing source files, because sources are migrated in the next task.

- [ ] **Step 4: Commit**

```powershell
git add airbot/build.gradle.kts settings.gradle.kts
git commit -m "build: add airbot module"
```

---

### Task 2: Migrate Character Sources Into Airbot Packages

**Files:**

- Create: `airbot/src/main/kotlin/com/airobot/airbot/character/DynamicEyes.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/character/RobotCharacter.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/conversation/DialogueBubble.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/conversation/TypewriterText.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/conversation/UserMessageBubble.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/voice/RobotVoiceInputPanel.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/voice/VoiceWaveform.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/state/Message.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/state/RobotStateEngine.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/state/RobotUiState.kt`

- [ ] **Step 1: Copy files from `character/src/main/kotlin/com/airobot/character` to `airbot/src/main/kotlin/com/airobot/airbot`**

Use the following source-to-target mapping:

```text
character/src/main/kotlin/com/airobot/character/comp/robot/DynamicEyes.kt -> airbot/src/main/kotlin/com/airobot/airbot/character/DynamicEyes.kt
character/src/main/kotlin/com/airobot/character/comp/robot/RobotCharacter.kt -> airbot/src/main/kotlin/com/airobot/airbot/character/RobotCharacter.kt
character/src/main/kotlin/com/airobot/character/comp/dialogue/DialogueBubble.kt -> airbot/src/main/kotlin/com/airobot/airbot/conversation/DialogueBubble.kt
character/src/main/kotlin/com/airobot/character/comp/dialogue/TypewriterText.kt -> airbot/src/main/kotlin/com/airobot/airbot/conversation/TypewriterText.kt
character/src/main/kotlin/com/airobot/character/comp/dialogue/UserMessageBubble.kt -> airbot/src/main/kotlin/com/airobot/airbot/conversation/UserMessageBubble.kt
character/src/main/kotlin/com/airobot/character/comp/voice/RobotVoiceInputPanel.kt -> airbot/src/main/kotlin/com/airobot/airbot/voice/RobotVoiceInputPanel.kt
character/src/main/kotlin/com/airobot/character/comp/voice/VoiceWaveform.kt -> airbot/src/main/kotlin/com/airobot/airbot/voice/VoiceWaveform.kt
character/src/main/kotlin/com/airobot/character/state/Message.kt -> airbot/src/main/kotlin/com/airobot/airbot/state/Message.kt
character/src/main/kotlin/com/airobot/character/state/RobotStateEngine.kt -> airbot/src/main/kotlin/com/airobot/airbot/state/RobotStateEngine.kt
character/src/main/kotlin/com/airobot/character/state/RobotUiState.kt -> airbot/src/main/kotlin/com/airobot/airbot/state/RobotUiState.kt
```

- [ ] **Step 2: Update package declarations**

Use these package declarations:

```kotlin
package com.airobot.airbot.character
```

for `DynamicEyes.kt` and `RobotCharacter.kt`.

```kotlin
package com.airobot.airbot.conversation
```

for `DialogueBubble.kt`, `TypewriterText.kt`, and `UserMessageBubble.kt`.

```kotlin
package com.airobot.airbot.voice
```

for `RobotVoiceInputPanel.kt` and `VoiceWaveform.kt`.

```kotlin
package com.airobot.airbot.state
```

for `Message.kt`, `RobotStateEngine.kt`, and `RobotUiState.kt`.

- [ ] **Step 3: Update intra-airbot imports**

Replace old imports:

```kotlin
import com.airobot.character.comp.robot.DynamicEyes
import com.airobot.character.comp.voice.VoiceWaveform
import com.airobot.character.state.ConversationSubState
import com.airobot.character.state.InteractionType
import com.airobot.character.state.Message
import com.airobot.character.state.MessageRole
import com.airobot.character.state.RobotEngineState
import com.airobot.character.state.RobotStateEngine
import com.airobot.character.state.RobotUiState
import com.airobot.character.state.RobotVisualState
```

with matching new imports:

```kotlin
import com.airobot.airbot.character.DynamicEyes
import com.airobot.airbot.voice.VoiceWaveform
import com.airobot.airbot.state.ConversationSubState
import com.airobot.airbot.state.InteractionType
import com.airobot.airbot.state.Message
import com.airobot.airbot.state.MessageRole
import com.airobot.airbot.state.RobotEngineState
import com.airobot.airbot.state.RobotStateEngine
import com.airobot.airbot.state.RobotUiState
import com.airobot.airbot.state.RobotVisualState
```

- [ ] **Step 4: Run airbot compile**

Run:

```powershell
.\gradlew.bat :airbot:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If import errors remain, update only imports that still reference `com.airobot.character`.

- [ ] **Step 5: Commit**

```powershell
git add airbot/src/main/kotlin/com/airobot/airbot
git commit -m "refactor: migrate character sources to airbot"
```

---

### Task 3: Migrate Conversation ViewModel And Add Audio Boundary Package

**Files:**

- Create: `airbot/src/main/kotlin/com/airobot/airbot/conversation/ConversationViewModel.kt`
- Create: `airbot/src/main/kotlin/com/airobot/airbot/audio/AirbotAudioController.kt`

- [ ] **Step 1: Copy and repackage `ConversationViewModel`**

Copy:

```text
character/src/main/kotlin/com/airobot/character/viewmodel/ConversationViewModel.kt
```

to:

```text
airbot/src/main/kotlin/com/airobot/airbot/conversation/ConversationViewModel.kt
```

Use this package declaration:

```kotlin
package com.airobot.airbot.conversation
```

Replace state imports with:

```kotlin
import com.airobot.airbot.state.ConversationSubState
import com.airobot.airbot.state.Message
import com.airobot.airbot.state.MessageRole
import com.airobot.airbot.state.RobotEngineState
import com.airobot.airbot.state.RobotStateEngine
```

Keep existing imports for `com.airobot.audio.*` and `com.airobot.core.comm.*`.

- [ ] **Step 2: Add `AirbotAudioController.kt` as an explicit boundary**

Create:

```kotlin
package com.airobot.airbot.audio

import com.airobot.audio.AudioService
import javax.inject.Inject

/**
 * Experience-level audio boundary for Airbot.
 *
 * Low-level recording, playback, KWS, Opus, and native code remain in :audio.
 * This class gives Airbot a stable package for future audio orchestration without
 * pulling native audio implementation into the feature module.
 */
class AirbotAudioController @Inject constructor(
    val audioService: AudioService
)
```

- [ ] **Step 3: Run airbot compile**

Run:

```powershell
.\gradlew.bat :airbot:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add airbot/src/main/kotlin/com/airobot/airbot/conversation/ConversationViewModel.kt airbot/src/main/kotlin/com/airobot/airbot/audio/AirbotAudioController.kt
git commit -m "refactor: move conversation orchestration to airbot"
```

---

### Task 4: Point App At Airbot

**Files:**

- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/com/airobot/tablet/airobotui/AiRobotMainScreen.kt`
- Modify: `app/src/main/kotlin/com/airobot/tablet/airobotui/viewmodel/MainShellViewModel.kt`

- [ ] **Step 1: Update `app/build.gradle.kts` dependency**

Replace:

```kotlin
implementation(project(":character"))
```

with:

```kotlin
implementation(project(":airbot"))
```

- [ ] **Step 2: Update `AiRobotMainScreen.kt` imports**

Replace these imports:

```kotlin
import com.airobot.character.comp.dialogue.DialogueBubble
import com.airobot.character.comp.voice.RobotVoiceInputPanel
import com.airobot.character.state.ConversationSubState
import com.airobot.character.state.InteractionType
import com.airobot.character.state.RobotEngineState
import com.airobot.character.state.RobotUiState
import com.airobot.character.state.RobotVisualState
import com.airobot.character.viewmodel.ConversationViewModel
import com.airobot.character.comp.robot.RobotCharacter
```

with:

```kotlin
import com.airobot.airbot.conversation.DialogueBubble
import com.airobot.airbot.voice.RobotVoiceInputPanel
import com.airobot.airbot.state.ConversationSubState
import com.airobot.airbot.state.InteractionType
import com.airobot.airbot.state.RobotEngineState
import com.airobot.airbot.state.RobotUiState
import com.airobot.airbot.state.RobotVisualState
import com.airobot.airbot.conversation.ConversationViewModel
import com.airobot.airbot.character.RobotCharacter
```

- [ ] **Step 3: Update `MainShellViewModel.kt` imports**

Replace:

```kotlin
import com.airobot.character.state.RobotEngineState
import com.airobot.character.state.RobotStateEngine
```

with:

```kotlin
import com.airobot.airbot.state.RobotEngineState
import com.airobot.airbot.state.RobotStateEngine
```

- [ ] **Step 4: Run app compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If Hilt aggregation reports duplicate bindings from `:character`, continue to Task 5 and remove `:character` from the build.

- [ ] **Step 5: Commit**

```powershell
git add app/build.gradle.kts app/src/main/kotlin/com/airobot/tablet/airobotui/AiRobotMainScreen.kt app/src/main/kotlin/com/airobot/tablet/airobotui/viewmodel/MainShellViewModel.kt
git commit -m "refactor: consume airbot from app"
```

---

### Task 5: Remove The Old Character Module

**Files:**

- Delete: `character/build.gradle.kts`
- Delete: `character/src/main/kotlin/com/airobot/character/**`

- [ ] **Step 1: Confirm no source imports still use `com.airobot.character`**

Run:

```powershell
Get-ChildItem app,airbot,audio,core,framework,services -Recurse -File -Include *.kt,*.kts | Select-String -SimpleMatch "com.airobot.character"
```

Expected: no output.

- [ ] **Step 2: Delete the old source module files**

Delete only:

```text
character/build.gradle.kts
character/src/main/kotlin/com/airobot/character/
```

Leave generated `character/build/` alone unless a cleanup task is explicitly requested, because generated build directories are not part of the source migration.

- [ ] **Step 3: Run full app compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add character/build.gradle.kts character/src/main/kotlin/com/airobot/character
git commit -m "refactor: remove character module sources"
```

---

### Task 6: Update Architecture Documentation

**Files:**

- Modify: `.agent/doc/architecture.md`
- Modify: `.agent/rules/airobot-rules.md`

- [ ] **Step 1: Update `.agent/doc/architecture.md` module tree**

Replace the old `character/` module description with:

```text
airbot/                       # Robot interaction feature module (Android Library)
  src/main/kotlin/com/airobot/airbot/
    character/                # Character visuals and expression components
    conversation/             # Conversation UI and ConversationViewModel
    voice/                    # Voice input panel and waveform UI
    audio/                    # Airbot-level audio orchestration boundary
    state/                    # Robot engine state, UI state, messages
```

Keep the existing `audio/` section, but clarify that it is the low-level audio engine:

```text
audio/                        # Low-level audio engine module (Android Library)
  src/main/kotlin/com/airobot/audio/
    player/                   # Audio playback
    recorder/                 # Audio recording and KWS pipeline
    tools/                    # Codec and KWS helpers
    AudioService.kt           # Reusable audio service API
    AudioServiceImpl.kt       # Reusable audio service implementation
  src/main/cpp/               # C++ JNI implementation
  src/main/assets/            # Offline speech/KWS model assets
```

- [ ] **Step 2: Update `.agent/doc/architecture.md` dependency notes**

Add this dependency note near the module-boundary section:

```markdown
- **Airbot (robot interaction feature)**: `airbot` owns character rendering, voice conversation UI, conversation orchestration, and robot dialogue state. It may depend on `audio`, `core`, `framework`, and `services`.
- **Audio (low-level engine)**: `audio` remains UI-free and reusable. It must not depend on `airbot`, `services`, or app-level state.
- **App Shell**: `app` composes product-level screens and consumes `airbot` as the robot interaction entry point.
```

- [ ] **Step 3: Update `.agent/rules/airobot-rules.md` responsibility rules**

Add or replace the module rule with:

```markdown
- **Robot interaction feature (`airbot`)**: owns character visuals, conversation UI, voice input UI, experience-level audio orchestration, and robot dialogue state. `app` should consume `airbot` instead of assembling `character` and conversation pieces directly.
- **Audio engine (`audio`)**: owns reusable recording, playback, KWS, Opus, native C++ integration, and audio assets. It must stay independent from `airbot` UI and robot state.
- **Foundation UI (`framework`)**: remains stateless and feature-independent. It must not depend on `airbot`, `app`, ViewModels, or top-level robot state.
```

- [ ] **Step 4: Search docs for stale `character` architecture references**

Run:

```powershell
Get-ChildItem .agent\\doc,.agent\\rules -Recurse -File -Include *.md | Select-String -SimpleMatch "character/"
```

Expected: no stale module-tree references to `character/`. Descriptive historical references are acceptable only if they explicitly say they are pre-migration context.

- [ ] **Step 5: Commit**

```powershell
git add .agent/doc/architecture.md .agent/rules/airobot-rules.md
git commit -m "docs: update architecture for airbot module"
```

---

### Task 7: Final Verification

**Files:**

- Verify: project source and Gradle configuration

- [ ] **Step 1: Verify no stale package imports**

Run:

```powershell
Get-ChildItem app,airbot,audio,core,framework,services -Recurse -File -Include *.kt,*.kts | Select-String -SimpleMatch "com.airobot.character"
```

Expected: no output.

- [ ] **Step 2: Verify module references**

Run:

```powershell
Get-ChildItem . -File -Include *.kts; Get-Content settings.gradle.kts; Get-Content app/build.gradle.kts
```

Expected:

```text
settings.gradle.kts includes :airbot
settings.gradle.kts does not include :character
app/build.gradle.kts depends on project(":airbot")
app/build.gradle.kts does not depend on project(":character")
```

- [ ] **Step 3: Run final compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Check git status**

Run:

```powershell
git status --short
```

Expected: only pre-existing unrelated changes remain, currently expected to include:

```text
 M gradle/libs.versions.toml
 M gradle/wrapper/gradle-wrapper.properties
```

- [ ] **Step 5: Final commit if needed**

If Tasks 1-6 were committed individually, no final commit is needed. If any verification-only fixes were made, commit them:

```powershell
git add <fixed-files>
git commit -m "fix: complete airbot migration"
```

---

## Self-Review

Spec coverage:

- New `:airbot` module: Task 1.
- Internal package layout for character, conversation, voice, audio, and state: Tasks 2 and 3.
- Keep `:audio` low-level and independent: Tasks 1, 3, and 6.
- Update `:app` to consume `:airbot`: Task 4.
- Remove or disable `:character`: Task 5.
- Update `.agent/doc` architecture documentation: Task 6.
- Verify build and stale imports: Task 7.

Placeholder scan:

- No unresolved placeholder text or vague implementation instructions remain.

Type consistency:

- New package roots consistently use `com.airobot.airbot`.
- `ConversationViewModel` moves to `com.airobot.airbot.conversation`.
- Robot state types move to `com.airobot.airbot.state`.
