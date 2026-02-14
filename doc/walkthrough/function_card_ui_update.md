
# Function Card UI Animation & Layout Update

## Overview
This document details the critical fix for the Voice Input Panel positioning.

## Changes

### 1. Robust Positioning: Centered on Robot
- **File**: `app/src/main/kotlin/com/airobotcomm/tablet/airobotui/AiRobotMainScreen.kt`
- **Previous Approach (Failed)**: Constrained the Voice Panel to the parent screen (start/end) and attempted to mimic the robot's movement using the same `horizontalBias` animation. This failed because differences in view widths caused the visual centers to be misaligned (off by ~120dp), and required perfect synchronization of bias values.
- **New Approach (Fixed)**: Constrained the Voice Panel DIRECTLY to the Robot's container (`robotRef`).
    - `start.linkTo(robotRef.start)`
    - `end.linkTo(robotRef.end)`
- **Result**: The Voice Panel is now mathematically forced to remain horizontally centered relative to the Robot. Wherever the Robot moves (animated bias or static position), the Voice Panel follows flawlessly. This eliminates alignment guesswork.

### 2. Internal Layout Review
- **Component**: `RobotVoiceInputPanel.kt`
- **Status**: The internal layout matches the visual requirement (Mic Button on top, text below). The removal of `fillMaxWidth` (in previous step) combined with the new external center-constraints ensures the component size is tightly wrapped and correctly positioned.

## Verification Steps

1.  **Open Function Card**:
    - Tap on a card.
    - **Observe**: The Robot slides left. The Voice Input Panel moves with it, staying perfectly centered beneath the robot's head.
    - **Outcome**: A cohesive "Character Unit" (Robot + Voice) is formed.

2.  **Relative Position Check**:
    - **Vertical**: Robot is biased slightly up (`verticalBias=0.5` within a constrained area). Voice Panel is anchored to the bottom with `65.dp` margin.
    - **Horizontal**: Centers are perfectly aligned.

## Build Verification
- Compile Check: **PASSED**
- Verify command: `.\gradlew.bat :app:compileDebugKotlin`
