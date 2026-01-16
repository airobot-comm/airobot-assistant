---
trigger: always_on
---

# AIRobot Android Tablet

一个基于AI的Android智能语音对话机器人的编程规则

## 编程规范

- 遵循java以及kotlin编程规范，严格类型检查
- 类及复杂方法需要简短注释：功能概要、关键点
- 文档要求使用中文输出

## 技术要求

- 代码架构遵循MVVM分层要求
- ui组件化设计，使用jetpack compose
- 通过Hilt DI机制解耦ui与服务调用

## 质量要求

- 复杂逻辑与业务模块需要单元测试
- 完成任务后需要review代码，消除lint的告警
- 完成任务自动运行编译，解决编译错误与告警

