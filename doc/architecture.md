# AIRobot Android Tablet

ai机器人项目Android版系统架构，技术设计等概要说明

## 📱 功能特性

- **实时语音**: 支持语音录制、实时传输和TTS播放
- **多轮对话**: 支持自动和手动两种对话模式
- **音频处理**: 集成Opus编解码、回声消除和降噪
- **状态管理**: 完整的对话状态流转和错误处理
- **WSocket**: 基于WebSocket的实时双向通信

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

## 🏗️ 项目架构

```
app/src/main/kotlin/com/airobotcomm/tablet/
├── audio/                    # 音频处理模块
│   ├── utils/                     # 音频基础功能
│   ├── EnhancedAudioManager.kt    # 增强音频管理器
│   └── OpusCodec.kt               # Opus编解码器
├── data/                     # 数据层 (Repository, Models)
│   ├── repository/                # Repository服务
│   ├── ConfigManager.kt           # 配置管理服务
│   └── DeviceConfig.kt            # 设备配置服务
├── network/                  # 网络通信模块
│   ├── di/                        # 网络hilt di服务
│   ├── protocol/                  # ota，机器人交互协议
│   ├── transport/                 # 底层ws长连接传输服务
│   └── NetworkService.kt          # 网络服务接口
│   └── NetworkServiceImpl.kt      # 网络服务接口实现
├── ui/                       # UI界面模块 (Presentation Layer)
│   ├── components/                # 可重用子模块组件
│   ├── framework/                 # ui框架如topbar，menu菜单...
│   ├── theme/                     # 主题配置
│   └── viewmodel/                 # 视图模型
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