# AIRobot Android Tablet

ai机器人项目Android版系统架构，技术设计等概要说明

## 📱 功能特性

- **角色管理**: airobot角色形象，支持微表情互动
- **实时语音**: 支持语音录制、实时传输和TTS播放
- **多轮对话**: 基于ai-Agent的自动语音对话模式
- **功能卡片**: 基于ai意图理解的功能卡片主动服务

## 架构设计

### 设计原则

- 分层架构，遵循clearArchitecture + MVVM要求
- ui要求组件化设计，并使用jetpack compose开发
- domain层采用模块化设计，模块内采用MVVM设计模式
- infra基础层提供网络API、数据库、音频/文件存储等
- 语音与协议通信模块独立，设计高性能，自愈合、高可靠
- 分层、业务模块间通过Hilt DI机制解耦，ui服务调用

### 项目架构
```
app/src/main/kotlin/com/airobotcomm/tablet/
├── airobotui/                # airobot单页UI层 (Presentation Layer)
│   ├── components/                # 可重用子模块组件
│   ├── framework/                 # ui框架如topbar，menu菜单...
│   ├── subpages/                  # 界面模块
│   ├── theme/                     # 主题配置
│   ├── viewmodel/                 # airobot viewmodel协调各业务状态    
├── audio/                    # 音频处理模块
│   ├── di/                        # audio模块hilt di服务
│   ├── utils/                     # 音频基础功能
│   ├── EnhancedAudioManager.kt    # 增强音频管理器
│   └── OpusCodec.kt               # Opus编解码器
├── comm/                    # 协议通信模块（多协议，多传输方式，自身维护）
│   ├── di/                        # comm通信模块hilt di服务
│   ├── protocol/                  # 机器人交互协议
│   ├── transport/                 # 底层ws，mqtt传输服务
│   └── CommService.kt             # 通信服务接口
│   └── commServiceImpl.kt         # 网络服务接口实现 
├──domain/                    # 业务逻辑层 (Domain Layer)
│   ├── di/                        # domain层hilt di服务
│   ├── ota/                      # ota激活与升级管理
│   └── robot/                    # 机器人配置管理   
├── infra/                    # infracture层 (Repository, network等)
│   ├── repository/                # Repository服务
│   ├── remote/                    # 远程数据仓库
│   └── model/                     # 数据模型 
├── utils/                    # 通用工具类
├── MainActivity.kt           # 主活动 (Activity)
└── RobotApplication.kt       # Hilt Application 入口
```

### Native模块
```
app/src/main/cpp/
├── opus_encoder.cpp         # Opus编码器JNI
├── opus_decoder.cpp         # Opus解码器JNI
└── CMakeLists.txt          # CMake构建配置
```

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

## 📚 文档

- [对话流程](./protocol/flow.md) - 对话流程实现和状态管理
- [通信协议](./protocol/protocol.md) - websocket协议
- [MCP协议](./protocol/mcp.md) - MCP工具调用协议

## 🔗 相关链接

- [Opus音频编解码](https://opus-codec.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)
- [Hilt (Dependency Injection)](https://developer.android.com/training/dependency-injection/hilt-android)