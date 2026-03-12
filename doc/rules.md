# AIRobot Android Tablet

AIRobot vibe_code规则

## 编程规范
- 遵循java以及kotlin编程规范，严格类型检查
- 类及复杂方法需要简短注释：功能概要、关键点
- 关键方法调用与事务通知节点增加调试日志log

## 交互要求
- 交互设计的原型参考：./doc/design/**
- ui开发见原型设计：../prototype/**

## 技术要求
- 分层架构，遵循clearArchitecture + MVVM要求
- ui要求组件化设计，并使用jetpack compose开发
- 语音与通信模块独立，设计要高性能，自愈合、高可靠
- System层负责ota认证，系统与ai机器人等的配置
- 各个业务模块通过Hilt DI机制解耦，ui服务调用
- 技术选型参考：architect/architecture.md

## 质量要求
- 复杂逻辑与业务模块需要注释，单元测试
- 完成任务后需review代码，消除lint告警
- 完成任务后运行编译并解决编译错误与告警