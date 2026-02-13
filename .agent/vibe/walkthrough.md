# Walkthrough - Resolve User Speech Transcript Visibility Bug

## 1. Overview
This bug fix ensures that the user's speech transcript is always visible during a conversation round, even if the server response (TTS) arrives faster than the STT transcription. 

## 2. Changes

### 2.1 ConversationViewModel.kt
Updated the event handling for `STT` and `TtsStart`:
- `handleSttResult`: Now explicitly handles STT packets arriving after TTS has already started. It ensures the text flow is updated while maintaining the current `SPEAKING` state.
- `handleTtsStart`: Added documentation regarding the server timing issue where TTS start might precede final STT.

### 2.2 AiRobotMainScreen.kt
Refactored the display of `UserMessageBubble` and general state management:
- **Consolidated State Updates**: Merged multiple `LaunchedEffect`s that were independently updating `robotUiState`. This eliminates race conditions where simultaneous updates (e.g., STT text arriving while state changes to SPEAKING) would overwrite each other.
- **Direct VM Linking**: The `UserMessageBubble` and `DialogueBubble` now consume `currentRoundUserText` and `currentRoundAiText` directly from the ViewModels. This ensures that the text shown in the bubble is always the latest source of truth, bypassing any UI-layer object synchronization delays.
- **Round-based Visibility**: Changed the visibility condition from a state-based check to a content-based check. The bubble stays visible throughout the conversation round and is cleared only when the ViewModel starts a new round.

## 3. Verification Result
- **Build**: Successfully compiled using `./gradlew :app:compileDebugKotlin`.
- **Logic**: Verified that the visibility lifecycle of the user bubble is now tied to the "Round" (reset at the start of a new round) rather than the "State" (which can fluctuate quickly).
