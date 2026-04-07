# AIRobot Android Tablet

AIRobot vibe_code规则

## 编程规范
- 遵循java以及kotlin编程规范，严格类型检查
- 类及复杂方法需要简短注释：功能概要、关键点
- 关键方法调用与事务通知节点增加调试日志log

## UI 设计与主题要求
- 交互设计的原型参考：./doc/design/**
- ui开发见原型设计：../prototype/**
- **双主题适配原则**：所有通用 UI 组件（背景、文字、边框、状态图标等）**严禁硬编码颜色**，必须一律引用 `RobotTheme.colors` 中的动态 Token。
- **IP 保护原则**：Aether 机器人主体的基色（脸部、五官）为固定色彩维持辨识度，其环境光晕（Aura）则需通过主题令牌与背景融合。

## 技术要求
- 分层架构，物理上采用多模块 (Multi-module) 隔离，遵循 ClearArchitecture + MVVM 要求
- UI 要求组件化设计，并使用 Jetpack Compose 开发
- **基础UI隔离 (`framework`)**：`framework` 模块提供无状态 (Stateless) 全局主题和组件库，严禁引入 `App` 层的组装 `ViewModel` 或顶级域状态模型 (如 `RobotEngineState`)。所有参数需设计为泛型字面量（Primitive Types）和事件回调。
- **业务完全独立**：语音 (`audio`)、主动服务卡片 (`services`) 模块等应当作为独立且跨应用层使用的子模块存在，包名要求通用（如 `com.airobot.services`），并维持内聚特性的状态原语 (`ServiceCardData`)，不与全局应用状态耦合。
- **App Shell 胶水主工程**：主 `app` 模块负责提供像 `SystemAuth`、OTA配置等业务页面的组装调用，利用 Hilt DI 机制结合 `framework` 组件库展示 `services` 和 `audio` 的功能能力。
- 技术选型参考：architect/architecture.md

## 质量要求
- 复杂逻辑与业务模块需要注释，单元测试
- 完成任务后需review代码，消除lint告警
- 完成任务后运行编译并解决编译错误与告警