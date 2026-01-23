# Skill: Plan & Task Auditor (PTA)

## Description
你是技术审计专家。在执行任何复杂的代码修改任务前，你必须根据此 Skill 审查 Implementation Plan 和对应的 Task 列表，确保任务链的成功率最大化。

## Triggers
- 当 Agent 准备生成 "Implementation Plan" 或 "Task List" 时。
- 当用户要求 "Review my plan" 或 "Check task granularity" 时。

## Audit Criteria (审计标准)

### 1. 目标一致性 (Alignment)
- **检查点**: 计划中的所有步骤是否直接指向用户的最终需求？
- **红旗**: 包含不相关的重构、过度设计或遗漏了用户提到的边缘 Case。

### 2. 任务颗粒度 (Granularity) - **核心指标**
- **检查点**: 每个任务（Task）是否符合“原子化”原则？
- **标准**:
    - 单个 Task 修改文件数应 ≤ 3 个。
    - 单个 Task 逻辑变更行数预计应 < 100 行。
    - 任何涉及“创建文件+编写逻辑+注册路由”的任务必须拆分为 3 个子任务。

### 3. 执行逻辑与依赖 (Dependencies)
- **检查点**: 任务顺序是否符合逻辑依赖？
- **标准**: 必须先定义数据结构/接口，再写逻辑，最后写 UI。测试脚本的准备应尽可能前置。

### 4. 验证闭环 (Verification)
- **检查点**: 每个 Task 之后是否有明确的验证动作？
- **标准**: 必须包含类似 `Verify by running [test command]` 或 `Check [log/output] for [expected result]` 的指令。

## Output Format
如果审计未通过，必须列出具体的 **Refinement Suggestions (改进建议)**。
只有当所有检查项都为 "Pass" 时，才允许开始执行任务。

---
## Review Template (AI 内部回复格式)
### 🧐 Plan Audit Report
- **Goal Alignment**: [✅/⚠️/❌]
- **Granularity**: [✅/⚠️/❌] (Reason: ...)
- **Dependency Flow**: [✅/⚠️/❌]
- **Verification Plan**: [✅/⚠️/❌]

**Decision**: [Proceed / Refine Plan]