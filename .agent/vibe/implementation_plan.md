# Implementation Plan - Fix User Speech Visibility

## 1. Revision History
| Date | Author | Description |
| :--- | :--- | :--- |
| 2026-02-12 | Antigravity | Initial plan to fix the late STT display bug. |

## 2. Problem Description
In the current implementation of the voice conversation flow:
- The server may send `ttsStart` before the final `STT` result message.
- `ConversationViewModel` transitions to `SPEAKING` state immediately upon receiving `ttsStart`.
- Subsequent `STT` results were being ignored or didn't trigger state changes correctly if they arrived during `SPEAKING`.
- `AiRobotMainScreen` only showed the `UserMessageBubble` during `LISTENING`, `THINKING`, or `SPEAKING` states, but it was strictly tied to the visual state machine which might have "skipped" or "missed" the window for display.

## 3. Technical Solution

### 3.1 ViewModel Layer (`ConversationViewModel`)
- **Late STT Handling**: Update `handleSttResult` to ensure `currentRoundUserText` is updated and messages are added even if the sub-state is already `SPEAKING`.
- **State Transition Guard**: Ensure that if an STT result arrives while already `SPEAKING`, we don't accidentally pull the state back to `THINKING`.
- **Proactive Speaking**: Keep the logic where `ttsStart` forces the state to `SPEAKING` to match audio output.

### 3.2 UI Layer (`AiRobotMainScreen`)
- **State Reconciliation Fix**: Consolidated multiple `LaunchedEffect`s that were independently updating `robotUiState`. Previously, concurrent updates from `robotState` and `currentRoundUserText` could cause one to overwrite the other with stale values (race condition).
- **Visibility Decoupling**: Modify the logic for displaying `UserMessageBubble`. Instead of checking if `visualState` is in `LISTENING/THINKING/SPEAKING`, we now show the bubble as long as `currentRoundUserText` is not empty.
- **Direct State Linking**: Linked the bubble components directly to the ViewModel's `currentRoundUserText` and `currentRoundAiText` flows, bypassing the local `robotUiState` aggregator for these critical high-frequency fields to ensure absolute transparency.
- **Round-based Cleanup**: Since `currentRoundUserText` is reset at the start of every new conversation round in the ViewModel, the bubble will naturally disappear when the next round begins.

## 4. Verification Plan
- [x] Compilation Check: Run `./gradlew :app:compileDebugKotlin` to ensure no syntax errors.
- [ ] Manual Test: Verify that during a fast-responding conversation, the user's transcript appears on screen even if the robot starts talking immediately. (To be done by USER/QA)
