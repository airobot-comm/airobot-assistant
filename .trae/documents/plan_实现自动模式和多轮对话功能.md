## 实现自动模式和多轮对话功能

### 问题分析

1. 对话状态（`ConversationState`）与UI组件使用的`RobotVisualState`需要同步
2. 自动模式下的多轮对话流程需要完善
3. 语音输入和AI输出时，语音与文字需要同步显示
4. 需要确保后端协议关闭对话时能正确处理

### 改进方案

#### 1. 统一状态管理

* 在`RobotConversationScreen`中添加状态映射逻辑，确保`ConversationState`与`RobotVisualState`同步

* 实现状态转换：`LISTENING` → `THINKING` → `SPEAKING` → 下一轮`LISTENING`（自动模式）

#### 2. 完善自动模式多轮对话

* 确保`startNextRound()`方法在TTS结束后正确调用

* 添加后端协议关闭对话的处理逻辑

* 实现`isAutoMode`状态的持久化管理

#### 3. 语音与文字同步

* 优化`handleTextMessage`方法，确保STT结果实时更新到UI

* 完善`DialogueBubble`组件，实现AI文字输出与语音播放的同步

* 增强语音波形动画，使其与实际语音输入强度关联

#### 4. 状态流转完整性

* 确保每轮对话的状态流转正确

* 添加状态转换的日志记录，便于调试

* 实现错误状态的优雅处理

### 具体实现步骤

1. **修改`AiRobotServiceScreen.kt`**：

   * 添加`ConversationState`与`RobotVisualState`的映射逻辑

   * 实现自动模式的启动和停止按钮

   * 确保UI状态与ViewModel状态同步

2. **优化`ConversationViewModel.kt`**：

   * 完善`startNextRound()`方法，添加状态检查

   * 添加后端协议关闭对话的处理逻辑

   * 确保语音输入和输出的状态正确更新

3. **增强`DialogueBubble.kt`**：

   * 实现AI文字输出与语音播放的同步

   * 添加TypewriterText效果与TTS播放的同步

4. **改进`EnhancedAudioManager.kt`**：

   * 添加语音输入强度检测，用于驱动波形动画

   * 优化音频事件处理，确保实时性

5. **更新`RobotVoiceInputPanel.kt`**：

   * 实现波形动画与实际语音输入的关联

   * 添加自动模式状态指示

### 预期效果

* 自动模式下，一次点击对话开始后，AI角色处于连续对话状态

* 每轮对话过程中，状态显示正确：倾听/人的说话输入 → 思考 → AI输出

* 语音输入时，波形动画与文字实时同步

* AI输出时，语音与文字同步显示

* 能正确响应后端协议关闭对话

### 测试要点

* 自动模式下的多轮对话流程

* 状态显示的正确性

* 语音与文字的同步性

* 后端协议关闭对话的处理

* 异常情况下的状态恢复

