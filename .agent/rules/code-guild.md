---
trigger: always_on
---

# AIRobot Assistant

AIRobot团队vibe_code执行规则，以及项目编程与技术规则

## VIBE规范

- Vibe讨论过程的Artifact文档要求英文输出，增加修订记录，支持增量更新
- 多轮comment对话Implementation Plan需增量更新，保留有效的历史信息
- Implementation Plan层次分明，需包括功能与改进点、技术方案概要设计
- 每轮计划变化，task都需要更新review及编译/lint错误告警检查任务状态
- fast（非play）模式的Vibe编程，也需要编译检查并输出walkthrough
- airobot项目与架构规范类文档统一储存并实时更新到：../../docs/*

## 编程规范
- 遵循java以及kotlin编程规范，严格类型检查
- **文件编码限制**：所有源文件（.kt, .java, .xml, .md）必须统一使用 **UTF-8 (No BOM)** 编码。严禁使用 GBK 或其他本地编码以防止乱码。
- **防止乱码 (Mojibake)**：在重构或使用自动化脚本处理代码时，务必检查输入输出流的字符集。若发现 `杩炴帴` 等乱码模式，应立即回滚并检查工具链。
- 类及复杂方法需要简短注释：功能概要、关键点
- 关键方法调用与事务通知节点增加调试日志log

## UI 设计与开发
- 交互设计原型参考：../../docs/design/**
- ui开发见原型设计：../../prototype/**
- UI 要求组件化设计，并使用 Jetpack Compose 开发
- **双主题适配原则**：所有通用 UI 组件（背景、文字、边框、状态图标等）**严禁硬编码颜色**，必须一律引用 `RobotTheme.colors` 中的动态 Token。
- **IP 保护原则**：Aether 机器人主体的基色（脸部、五官）为固定色彩维持辨识度，其环境光晕（Aura）则需通过主题令牌与背景融合。

## 技术与架构设计
- 技术选型参考：architect/architecture.md
- 分层架构，物理上采用多模块 (Multi-module) 隔离，遵循 ClearArchitecture + MVVM 要求
- **基础UI隔离 (`framework`)**：`framework` 模块提供无状态 (Stateless) 全局主题和组件库，严禁引入 `App` 层的组装 `ViewModel` 或顶级域状态模型 (如 `RobotEngineState`)。所有参数需设计为泛型字面量（Primitive Types）和事件回调。
- **业务逻辑下沉 (`core`)**：`core` 模块负责核心协议 (`comm`) 与系统管理 (`system`)。包含设备激活、OTA、AI Agent 握手等核心业务逻辑，为 `app` 层提供统一的 `SysManage` 接口。
- **业务完全独立**：语音 (`audio`)、主动服务卡片 (`services`) 模块等应当作为独立且跨应用层使用的子模块存在，包名要求通用（如 `com.airobot.services`），并维持内聚特性的状态原语 (`ServiceCardData`)，不与全局应用状态耦合。
- **App Shell 胶水主工程**：主 `app` 模块负责顶层组装，通过 `MainShellViewModel` 协调 `core` (系统/通讯)、`airbot` (角色) 与 `audio` (音频) 之间的状态流转，利用 Hilt DI 注入具体实现。

## 质量要求
- 复杂逻辑与业务模块需要注释，单元测试
- 完成任务后需review代码，消除lint告警
- 完成任务后运行编译并解决编译错误与告警
