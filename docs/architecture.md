# AIRobot Assistant Architecture

ai机器人Android Assistant系统架构，技术设计等概要说明

## 📱 功能特性

- **角色管理**: 支持多卡通 IP 角色展示与实时切换，支持微表情与情感动画联动。
- **全语音交互**: 离线唤醒词监听（KWS）、实时人声检测（VAD），支持全双工实时**语音打断**。
- **多轮对话**: 基于端侧 Agent 编排的**连续多轮对话**，无需重复唤醒。
- **主动服务卡片**: 基于 Agent 意图识别，支持番茄专注时钟、AI 日程、以及**播客 DIY 创作卡片**、**音视频播客播放卡片**的主动下发与交互。

## 架构设计

### 设计原则

- 采用多模块化 (Multi-module) + MVVM 的 Clear Architecture，将复用组件和核心逻辑分包隔离。
- 核心 UI 组件全部声明式设计，使用 Jetpack Compose 进行开发。
- 智能体编排与实时语音 (agent) 模块与底层协议通信 (core) 模块物理隔离，核心音频算法使用 Native C++ 编写以保证高性能与高可靠。
- 角色表现层 (`airbot`) 独立化，支持 Canvas/Rive 多引擎渲染，以纯组件形式向 App 壳层提供服务。
- 业务逻辑与卡片功能（`:features`）高内聚，具有自包含的状态控制，支持按需扩展。
- 各个业务模块间通过 Hilt DI 依赖注入机制进行全局解耦和组装。

### 项目架构
```text
airobot-assistant/
├── app/                          # 主壳工程模块 (App Shell)
│   ├── src/main/kotlin/com/airobot/assistant/
│   │   ├── apppages/             # UI 业务组装 (时钟、设置、主交互视窗)
│   │   │   ├── settings/         # 系统设置与认证页面
│   │   │   ├── viewmodel/        # 主壳 UI ViewModels (MainShellViewModel)
│   │   │   └── AppMainScreen.kt  # 顶层布局拼装
│   │   ├── MainActivity.kt
│   │   └── RobotApplication.kt
└── libs/                         # 核心功能模块物理分发目录
    ├── framework/                # 🎨 基础 UI 框架模块 (Android Library)
    │   └── src/main/kotlin/com/airobot/framework/
    │       ├── comp/             # 跨业务通用 UI 无状态组件 (对话框、按钮等)
    │       ├── theme/            # 全局 RobotTheme 主题引擎 (色彩 Token、排版等)
    │       └── statusbar/        # 全局无状态系统栏组件
    ├── features/                 # 🧩 独立业务功能与服务卡片模块 (Android Library)
    │   └── src/main/kotlin/com/airobot/features/
    │       ├── clock/            # 番茄时钟、事务闹钟
    │       ├── schedule/         # 日程日历、备忘录
    │       ├── podcast/          # 播客 DIY 创作卡片、音视频播放面板卡片 (Media3)
    │       └── aiprovider/       # 具体 AI 服务对接组件
    ├── core/                     # 📡 核心协议通信与系统管理模块 (Android Library)
    │   └── src/main/kotlin/com/airobot/core/
    │       ├── comm/             # 网络通信逻辑 (WebSocket 传输、通信协议包)
    │       └── system/           # 系统配置、激活认证与 OTA 升级库
    ├── airbot/                   # 👽 虚拟角色视觉动效渲染模块 (Android Library)
    │   └── src/main/kotlin/com/airobot/airbot/
    │       ├── character/        # 多引擎视觉动效组件 (Canvas 纯绘制 + Rive 卡通)
    │       ├── dialogue/         # 气泡与对话 UI 组件
    │       ├── state/            # 角色自身领域状态与模型
    │       └── viewmodel/        # 互动状态及角色动作调度 ViewModel
    └── agent/                    # 🧠 AI 智能体大脑与实时音频处理引擎 (Android Library)
        ├── src/main/kotlin/com/airobot/agent/
        │   ├── brain/            # 智能体心智逻辑、大模型调度及技能分发编排
        │   ├── audio/            # 实时录音与播放驱动、VAD/KWS 检测与打断/多轮会话管理器
        │   └── skills/           # 智能体专用能力扩展接口与实现
        ├── src/main/cpp/         # Native 音频处理层 (Opus 编解码、降噪与回声消除 C++ 核心)
        └── src/main/assets/      # 离线唤醒词(KWS)与人声活性检测(VAD)模型文件
```

### 业务结构与组装层解耦

- **Framework ( UI 基础库 - `:framework` )**:
  作为最底层的无状态 UI 基础组件库，**严禁引入任何业务层逻辑、ViewModel 依赖或全局状态模型**。它通过 `RobotTheme` 提供动态明暗色彩 Token，只接收泛型/原始数据类型（Primitive Types）与回调事件，从而保证极高的通用性与纯粹度。
- **Features ( 业务与功能卡片 - `:features` )**:
  内置了番茄时钟、日程日历、以及基于 Media3 框架的播客 DIY、音视频播客播放等卡片服务。采用高内聚、自包含的局部状态设计，通过暴露标准的回调与外部交互，与全局顶级状态解耦。
- **Core ( 协议通讯与系统 - `:core` )**:
  负责设备激活认证、安全加签、远程 OTA 配置更新，以及底层的 WebSocket 长连接通道数据传输和通信协议包编解码，提供底层的网络连接基础设施。
- **Airbot ( 卡通角色渲染 - `:airbot` )**:
  负责卡通 IP 形象的视觉动画渲染，支持 Canvas 与 Rive 引擎组件。通过接收外部输入的状态机定义（闲置、聆听、思考、说话等状态）来展示对应的微表情和动画，避免环形依赖具体的业务逻辑。
- **Agent ( 智能体大脑与实时音频 - `:agent` )**:
  系统的“听觉与思考”核心。将 Agent 编排心智（大脑、技能集、大模型上下文）与实时音频交互流水线合并在统一模块下，底层通过 NDK 驱动 JNI 调用 C++ 编解码器（Opus），并结合离线 KWS 和 VAD 模型，直接驱动全双工实时语音、声音播放以及语音打断/连续多轮对话的逻辑。
- **App Shell ( 主工程壳 - `:app` )**:
  整个项目的集成胶水，作为 Hilt 依赖注入的总入口，使用 `MainShellViewModel` 接收并协调底层 `:core` 通信状态、`:agent` 智能体音频状态和 `:airbot` 角色动画状态的流转。

## 🛠️ 技术设计

### 核心框架
- **Kotlin**
- **Jetpack Compose**
- **Hilt**: 依赖注入框架，管理应用级的依赖关系。

### 🎨 主题与设计系统 (Design System)
- **多主题动态切换**: 基于自定义 `CompositionLocal` 配合 `MaterialTheme` 构建完整的明暗主题 (Light/Dark Mode) 支持架构。
- **语义化 Token 体系**: 隔离并提取 `cardBg`, `textPrimary`, `surfaceOverlay`, `accent` 等 Token，UI 层禁止产生任何色彩硬编码（Hardcoding）。
- **动效融合交互**: Aether IP 卡通部件保持高保定 IP 固定色值保护，周围光爆（Aura）等视觉环境光利用混合模型动态适配全局主题上下文环境。

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

- [对话流程](protocol/flow.md) - 对话流程实现和状态管理
- [通信协议](protocol/protocol.md) - websocket协议
- [MCP协议](protocol/mcp.md) - MCP工具调用协议

## 🔗 相关链接

- [Opus音频编解码](https://opus-codec.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)
- [Hilt (Dependency Injection)](https://developer.android.com/training/dependency-injection/hilt-android)
