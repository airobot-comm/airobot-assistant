<div align="center">

<img src="./docs/assets/logo.webp" width="128" height="128" alt="AiRobot Logo" />

# AiRobot Assistant

![AiRobot Assistant Logo](https://img.shields.io/badge/AiRobot-Assistant-blue?style=for-the-badge)
![Platform](https://img.shields.io/badge/platform-Android-green?style=for-the-badge)
![API](https://img.shields.io/badge/API-24%2B-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-purple?style=for-the-badge)

🌐 **[简体中文](README_zh.md) | [English](README.md)**

**An energetic animated interactive and full voice interaction airobot system based on multi-agent platforms**

[📖 Introduction](#-introduction) • [✨ Core Features](#-core-features) • [📱 Application Scenarios](#-application-scenarios) • [🔖 How to Use](#-how-to-use) • [🚀 Development Guide](#-development-guide) • [🌍 AiRobot Community](#-airobot-community)

</div>

---

## 📖 Introduction

**AiRobot Assistant** is a highly customizable robot client system that seamlessly integrates with leading Agent platforms (such as Xiaozhi AI, Coze, Dify, etc.). Designed specifically for Android tablets and desktop robots with screens, this project enables developers to build custom applications for scenarios like learning companionship, office assistance, eldercare, and more.

Different from traditional voice assistants, this project leverages cutting-edge LLMs and centers around **"dynamic emotional interaction"** and **"proactive scenario services"**. With vibrant and delicate animated IP character behaviors, lightning-fast full-duplex voice interactions, and dynamic on-demand extension cards, it brings warm, immersive "digital life" companion experiences to users. AiRobot breaks the limitation of one-way Q&A and elevates the interaction to proactive service delivery, aligning closer to users' real needs.

### 🎯 Core Positioning

- **🤖 Multi-Engine Agent Base**: Decoupled design that supports flexible attachment of diverse Agent platforms (e.g. Xiaozhi AI, Coze, Dify), allowing access to top-tier models and ecosystems.
- **🎭 Immersive Digital Life**: Presented via delicate and expressive 3D/2D animation IP characters, breathing life into AI and providing a true sense of physical companionship beyond mere text or static voice.
- **🗣️ Full-Voice Interaction**: Full-duplex and full-voice mechanisms featuring instant wake-up, streaming recording, real-time voice recognition, and bidirectional interruption.
- **🧩 Dynamic Card Services**: A pioneering proactive service card loop. In conversations, the AI not only replies via voice but also actively pushes service cards (e.g. Tomato Timer, weather reminders, AI memos) to users.

### 📸 Application Screenshots

<div align="center">
  <table style="width: 100%; border-collapse: collapse;">
    <tr>
      <td align="center" style="width: 33%;">
        <b>Home Page</b><br/>
        <img src="./docs/design/home_page.png" width="100%" />
      </td>
      <td align="center" style="width: 33%;">
        <b>Dialogue Bubble</b><br/>
        <img src="./docs/design/speak_bubble.png" width="100%" />
      </td>
      <td align="center" style="width: 33%;">
        <b>Function Cards</b><br/>
        <img src="./docs/design/function_card.png" width="100%" />
      </td>
    </tr>
  </table>
</div>

---

## ✨ Core Features

### 🎭 Animation Interaction
- **Multi-Role IP Representation**: Supports seamless switching between 3D/2D animated characters, with highly customizable character visual styles and interactive behaviors.
- **Dynamic Emotional Expressions**: Characters feature smooth transitions across states (listening, thinking, speaking, idling), naturally aligning with semantic emotional cues to avoid robotic interfaces.
- **Enhanced Visual Feedback**: Offers rich visual micro-interactions for states such as network exceptions, wake-word listening, processing, etc., ensuring system feedback is easily perceived.

### 🗣️ Full Voice Interaction
- **Fast Offline Wake-up**: Integrates efficient Keyword Spotting (KWS) to immediately respond to wake-words and transition to active dialogue.
- **Intelligent VAD**: Uses highly accurate Voice Activity Detection (VAD) for automatic phrase segmentation and silence detection.
- **Voice Interruption**: Supports real-time interruption while the robot is speaking, enabling natural, flowy turn-taking.
- **Continuous Conversation**: Supports continuous multi-round dialogue after waking up, eliminating the need to repeat the wake-word for every query.
- **Noise Suppression & Echo Cancellation**: Integrates hardware-assisted AEC and gain-control optimizations to ensure accurate voice recognition in far-field or noisy environments.

### 🧩 On-demand Function Card Services
- **Automatic Service Delivery**: Once the Agent recognizes the user's intent, it can push interface cards via the custom protocol, achieving a "what you talk is what you get" experience.
- **All-Scenario Business Templates**: Built-in and extensible card modules, currently including Focus Tomato Timer, AI Memo, Task Alarm, Podcast DIY, Audio Podcast, Video Podcast, and more.
- **Infinite Extensibility**: Powered by a protocol layer mimicking MCP, developers can quickly add various utility or lifestyle modules to the robot's brain.

---

## 📱 Application Scenarios

### 🏠 Smart Desktop & Information Display
- **Aesthetic Desk Clock**: Serves as a personalized desk companion displaying clock face, weather updates, and dynamic IP character visual animations.
- **Office Assistant**: Quickly set mindmaps, log schedules, and launch tomato timers using voice.

### 👶👧 Children Companionship & Education
- **All-Day AI Companion**: Patiently answers children's questions, using dynamic panels to visualize complex educational topics.
- **Emotional Support**: Provides friendly, concrete character animations and gentle voice responses to offer positive mental guidance and growth companionship.

### 🏢 Store Welcome & Customer Guide
- **Interactive Receptionist**: Deployed at front desks or exhibition halls, greeting customers with dynamic gestures and full-duplex voice to introduce featured products.
- **Visual Guiding**: Actively opens info/graphics cards based on voice inquiries to drive conversions.

### 🧓 Eldercare & Companionship
- **Daily Reminders**: Uses large fonts and clear voice cues to present medication reminders and daily news cards.
- **Accessible Conversations**: Overcomes barriers of touchscreen menus with fully fluid voice chat, providing zero-barrier conversations.

---

## 🔖 How to Use

Mainly consists of the following steps (current system supports Xiaozhi AI Agent):

1. **Step 1: System Device Activation**
   After installing the app, navigate to "System Settings" -> "System Authentication" and click to activate the device. **Note: Currently the system only supports Xiaozhi AI Agent** (support for Coze/JoyAgent, Dify, etc., is planned in subsequent releases).
   
   <img src="./docs/design/system_auth.png" width="600" alt="System Auth Info" />

2. **Step 2: Xiaozhi Backend Agent Binding**
   Retrieve the "Device AI Agent Activation Code" (e.g. `642225`) displayed on the screen. Go to the Xiaozhi AI Console and add this activation code to your configured Agent settings to bind the device.

   <img src="docs/design/agent_activation.png" width="600" alt="Device AI Agent Activation" />

3. **Step 3: Wake Up and Converse**
   Directly wake up the robot using the wake-word (e.g. "Xiao Ye, Xiao Ye") to start chatting.

4. **Step 4: Smart Podcast Experience (Creation & Playback)**
   - **Podcast Creation (DIY Card)**: Issue a voice request like "I want to create a podcast about AI technology", and the AI will push a "Podcast DIY Card". Customize the topic, hosts' genders, and style on the card, and click to submit.
   - **Podcast Playback (Audio Playback)**: Once generated, the system launches the "Podcast Player Card", playing your custom podcast via our high-performance media subsystem. The character's animation will adjust dynamically to the audio.

> **💡 Note**: For details about Xiaozhi AI Agent configurations, please refer to the official Xiaozhi AI manuals.
> - **Xiaozhi AI Console**: [xiaozhi.me](https://xiaozhi.me)

---

## 🚀 Development Guide

### 📋 Environment Requirements
- **IDE**: Android Studio latest stable version (Koala or newer recommended)
- **Android SDK**: API 34/35+, Android Gradle Plugin 9+
- **Languages/Tools**: Kotlin 2.0+, Android 11.0+ (API 30+) devices recommended for optimal performance

### 📦 Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/ronnieshi79/airobot-assistant.git
   cd airobot-assistant
   ```
2. **Import and Configure**
   - Open the directory in Android Studio.
   - Reference `keystore/` settings to set your signing configurations (or configure environment variables following instructions in `.agent/rules/code-guild.md`).
3. **Build & Run**
   - Connect your Android tablet or desktop hardware.
   - Compile and deploy the `app` module by clicking **Run** in Android Studio.
   - Set up the Agent backend URL, wake the robot up, and enjoy the full-duplex voice experience.

### 📅 Roadmap

- [x] **v1.0: Expression Engine & Unidirectional Integration Base**
  - [x] Establish base architecture and project Vibe flow standards
  - [x] Implement multi-role animated expression layer based on ViewModel state machines
  - [x] Synthesize TTS audio replies and establish baseline dialogue loops
  - [x] Develop on-demand function card base and template demos (Tomato Timer, Text Cards)
- [x] **v1.1: Architecture Optimization & Interruption Mechanisms**
  - [x] Refactor UI architecture, unify AiRobot state flows, and improve role extension flexibility
  - [x] Support real-time Voice Interruption using dual-engine KWS and VAD
  - [x] Support Continuous Conversation, drastically improving speech interaction continuity
- [x] **v1.2: Multi-Role Switch & Agent Base**
  - [x] Introduce decoupled AI Agent bases and mind orchestration engines (`agent` module)
  - [x] Support Multi-Role configurations, enabling users to choose and switch characters in real time (`airbot` module)
  - [x] Extend function card ecosystem to support Podcast DIY, Audio/Video Podcast card triggers
- [ ] **v2.0: Multi-Platform Ecosystem & MCP Open Services**
  - [ ] Connect mainstream Agent platforms (Coze / Dify APIs and WebSockets)
  - [ ] Enhance MCP service capabilities, allowing third-parties to upload card-based micro-apps
  - [ ] Expand 3D character physics reactions and emotion/expression libraries
- [ ] **v3.0: Hardware Terminal Ecosystem**
  - [ ] Build customization plans for white-label screens and deep OS level packaging

### 📚 Project Documents

Explore the codebase architecture and engineering norms:

- **[Project Rules]**: [`.agent/rules/code-guild.md`](.agent/rules/code-guild.md) - Project Vibe Code rules and code style guides
- **[Technical Architecture]**: [`docs/architecture.md`](docs/architecture.md) - Code architecture and component overview
- **[System Prototype]**: [`prototype/`](prototype) - Interactive Web-based prototype designs
- **[UI Design Assets]**: [`docs/design/`](docs/design) - Visual drafts and UI reference images
- **[Communication Protocols]**: [`docs/protocol/protocol.md`](docs/protocol/protocol.md) - Complete documentation on WebSocket commands and messages between AiRobot and the Agent

---

## 🌍 AiRobot Community

**AiRobot Community** connects geeks, hardware providers, and industry solution developers. Welcome to join us and build desktop companion robots, eldercare assistants, smart welcoming displays, and share in the AI robot co-creation journey!

### 🎯 Community Goals
- **Build Open-Source Digital Life**: Empower hardware with animated companionship and voice capabilities, bringing digital life to millions of families.
- **Hardware & Algorithm Ecosystem**: Explore the hardware form factors of AI desktop robots and build the physical companion interfaces of tomorrow.
- **Empower Vertical Industries**: Bridge advanced LLM agents to physical education, rehabilitation, and retail scenes.

### 🤝 Commercial Partnerships
- **Full-Stack Partnership Plan**: We offer commercial editions and flexible partnerships for integrators and hardware makers.
- **Edge Intelligence**: The commercial version comes with proprietary local agent engines, ensuring data privacy while supporting custom active function cards.
- **Custom Solutions**: Provide turn-key solutions from character modeling to core dialog orchestration, facilitating rapid go-to-market.

### 🤝 Join Us
- **🔗 Stay Tuned**: [AiRobot Community - Xiaohongshu](https://www.xiaohongshu.com/user/profile/5c2851dc0000000007038e53)
- **💡 Contributing & Collaboration**: We welcome code commits, hardware configurations, and scenario partnerships! Check [**CONTRIBUTING.md**](./CONTRIBUTING.md) to start.

---

<div align="center">
  <p>Create life with mind, warm life with technology.</p>
</div>
