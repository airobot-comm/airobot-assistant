# AiRobot Android Tablet

AiRobot Android tablet智能对话,场景服务应用。

## 📱 功能特性

- **文本交互**: 支持文本输入和显示
- **多轮对话**: 支持自动和手动两种对话模式
- **WebSocket通信**: 基于WebSocket的实时双向通信
- **状态管理**: 完整的对话状态流转和错误处理

## 使用说明

在设置中配置OTA地址，通过OTA下发的验证码与后端绑定激活，激活成功后，即可进行对话。

OTA地址先使用小智官方地址：  
https://api.tenclass.net/xiaozhi/ota/

也可自行搭建后端服务：  
[https://github.com/xinnan-tech/xiaozhi-esp32-server](xiaozhi-esp32-server)

## 📚 文档

- [详细介绍](./doc/description.md) - 项目的详细介绍
- [API通信协议](./doc/api.md) - WebSocket通信协议详细说明
- [对话流程](./doc/flow.md) - 对话流程实现和状态管理
- [MCP协议](./doc/mcp.md) - MCP工具调用协议
- [OTA接口文档](./doc/OTA%20接口文档.pdf) - OTA接口文档（[线上版](https://ccnphfhqs21z.feishu.cn/wiki/FjW6wZmisimNBBkov6OcmfvknVd)）
- [通信协议：Websocket连接](./doc/通信协议：Websocket%20连接.pdf.pdf) - 通信协议：Websocket连接（[线上版](https://ccnphfhqs21z.feishu.cn/wiki/M0XiwldO9iJwHikpXD5cEx71nKh)）