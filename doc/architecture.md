# AIRobot Tablet Architecture

ai机器人Android Tablet系统架构，技术设计等概要说明

## 📱 功能特性

- **角色管理**: airobot卡通角色，支持微表情互动
- **实时语音**: 支持vad，语音录制、传输和TTS播放
- **多轮对话**: 基于ai-Agent的自动语音对话模式
- **功能卡片**: 基于ai意图理解的功能卡片主动服务

## 架构设计

### 设计原则

- 采用多模块化 (Multi-module) + MVVM 的 Clear Architecture
- AiRobotUi 组件化设计，使用 Jetpack Compose 开发
- 语音 (`audio`) 与协议通信 (`comm`) 模块物理隔离，设计高性能、自愈合、高可靠
- 系统管理模块负责系统配置与 OTA 管理、角色配置等功能
- 各个业务模块间通过 Hilt DI 机制解耦调用

### 项目架构
```
airobot-tablet/
├── app/                          # 主壳工程模块 (App Shell)
│   ├── src/main/kotlin/com/airobot/tablet/
│   │   ├── airobotui/            # airobot单页UI层 (Presentation Layer)
│   │   ├── comm/                 # 协议通信业务实现
│   │   ├── service/              # service卡片容器与功能卡片组件
│   │   ├── system/               # 系统管理业务实现
│   │   ├── MainActivity.kt
│   │   └── RobotApplication.kt
├── audio/                        # 🎙️ 音频处理子模块 (Android Library)
│   ├── src/main/kotlin/com/airobot/audio/
│   │   ├── player/               # 音频播放
│   │   ├── recorder/             # 音频录制与 KWS
│   │   ├── tools/                # 编解码实现 (Opus)
│   │   ├── AudioService.kt       # 通用音频服务接口
│   │   └── AudioServiceImpl.kt   # 接口实现
│   ├── src/main/cpp/             # C++ JNI 实现
│   └── src/main/assets/          # 语音识别/唤醒离线模型
└── agent/                        # 🧠 AI 智能体模块 (Future: Rust Integration)
```

### 语音模块 (audio module)
- **解耦设计**: 通用路径 `com.airobot.audio`，可供不同终端复用。
- **性能优化**: C++ 核心逻辑下沉，减少对 JVM 依赖。
- **资源隔离**: 携带独立的离线 AI 模型资源，不占用主包编译资源空间。

## 🛠️ 技术设计

### 核心框架
- **Kotlin**
- **Jetpack Compose**
- **Hilt**: 依赖注入框架，管理应用级的依赖关系。

### 网络通信
- **OkHttp**: 客户端HTTP和WebSocket通信
- **Kotlinx Serialization**: 协议层JSON数据序列化与反序列化
- **Gson**: JSON数据处理

### 音频处理
- **Opus编解码**: 1.3.1 - 高质量音频压缩
- **Native C++**: CMake + NDK音频处理
- **AudioRecord/AudioTrack**: Android原生音频API
- **回声消除**: AcousticEchoCanceler
- **降噪处理**: Noise Library (基于com.github.paramsen:noise)

### 异步处理
- **Kotlin Coroutines**
- **Flow**
- **ViewModel**

### 导航
- **Navigation Compose**: 2.9.6 - 声明式导航

## 📋 系统要求

- **Android版本**: Android 10 (API 29) 及以上
- **权限要求**:
  - `RECORD_AUDIO` - 录音权限
  - `INTERNET` - 网络访问
  - `ACCESS_NETWORK_STATE` - 网络状态
  - `MODIFY_AUDIO_SETTINGS` - 音频设置

## 📚 基于小智ai的对话通信协议

- [对话流程](./protocol/flow.md) - 对话流程实现和状态管理
- [通信协议](./protocol/protocol.md) - websocket协议
- [MCP协议](./protocol/mcp.md) - MCP工具调用协议

## 🔗 相关链接

- [Opus音频编解码](https://opus-codec.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)
- [Hilt (Dependency Injection)](https://developer.android.com/training/dependency-injection/hilt-android)