
# Function Card UI Animation & Layout Update

## Overview
This update refines the UI animation and layout for the function card display mode, ensuring a clear separation between the conversation interface and the function card prototype.

## Changes

### 1. Decoupled Conversation from Function Card
- **File**: `app/src/main/kotlin/com/airobotcomm/tablet/airobotui/AiRobotMainScreen.kt`
- **Change**: The `FunctionalModulePanel` no longer receives or displays conversation text (`aiMsg`). It now strictly displays the function card UI (prototype).
- **Impact**: Conversation text remains in the robot's speech bubble, preventing it from flowing into the function card area.

### 2. Enabled Simultaneous Display
- **File**: `app/src/main/kotlin/com/airobotcomm/tablet/airobotui/AiRobotMainScreen.kt`
- **Change**: The `DialogueBubble` is now always displayed if there is conversation text, regardless of whether a function card is open.
- **Layout**: The robot and its bubble slide to the left, while the function card appears on the right.

### 3. Adjusted Robot Position
- **File**: `app/src/main/kotlin/com/airobotcomm/tablet/airobotui/AiRobotMainScreen.kt`
- **Change**: The horizontal bias for the robot in "Card Mode" was adjusted from `0.15f` to `0.08f`.
- **Visual**: This moves the robot further to the left edge, providing better separation from the function card on the right.

### 4. Persisted Card Mode
- **File**: `app/src/main/kotlin/com/airobotcomm/tablet/airobotui/state/RobotUiState.kt`
- **Change**: Updated `isInteracting` to return `true` if `activeCard` is not null.
- **Impact**: The UI remains in "Card Mode" (split screen) even if the conversation ends (robot becomes IDLE), ensuring the function card doesn't close unexpectedly unless explicitly dismissed.

## Verification Steps

1.  **Open Function Card**:
    - Click on any function card in the carousel (right side).
    - **Expected**: The robot avatar slides smoothly to the left (`bias 0.08`). The function card panel expands on the right.

2.  **Trigger Conversation**:
    - Speak to the robot or trigger a mock conversation.
    - **Expected**: The conversation bubble appears next to the robot (left side). Text appears in the bubble. Data does NOT appear in the function card.
    - **Reference**: Matches the design intent where conversation and function are visually distinct but active simultaneously.

3.  **End Conversation**:
    - Let the conversation timeout or stop.
    - **Expected**: The robot returns to IDLE state visually, but REMAINS on the left side. The function card stays OPEN.

4.  **Close Card**:
    - Click the 'X' button on the function card.
    - **Expected**: The card closes, and the robot slides back to the center (`bias 0.5`).

## Build Verification
- Compile Check: **PASSED**
- Verify command: `.\gradlew.bat :app:compileDebugKotlin`
