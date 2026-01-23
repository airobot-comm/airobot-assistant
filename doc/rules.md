# AIRobot Android Tablet

AIRobot Tablet vibe_code规则

## 编程规范
- 遵循java以及kotlin编程规范，严格类型检查
- 类及复杂方法需要简短注释：功能概要、关键点
- 关键方法调用与事务通知节点增加调试日志log
- MD文档与及Ai编程Artifact产出要求中文输出

## 交互要求
- 交互设计的原型参考：./doc/design/**
- ui开发见原型设计：../prototype/**

## 技术要求
- 代码架构遵循分层设计，参考MVVM要求
- ui组件设计使用jetpack compose
- 语音与网络服务，高内聚，内建高性能，自恢复机制
- 语音，网络等服务通过Hilt DI机制解耦ui服务调用
- 技术选型参考：architect/architecture.md

## 质量要求
- 复杂逻辑与业务模块需要单元测试
- 完成任务后需review代码，消除lint告警
- 完成任务后运行编译并解决编译错误与告警