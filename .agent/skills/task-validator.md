# Skill: Implementation task Auditor

## Context
当 Agent 更新 Implementation Plan的task列表时，自动触发此 Skill 进行审计。

## Review Criteria (审计准则)
1. **任务对齐度 (Alignment):** 检查 Task 列表是否覆盖了 Plan 中提到的所有技术点，是否存在“漏掉的 TODO”。
2. **原子性检查 (Atomicity):** 每个 Task 是否只做一件事？（原则：修改超过 3 个文件或 100 行代码的任务必须拆分）。
3. **依赖合理性:** 检查 Task 顺序。例如：是否在定义 API 之前就去写 UI 调用？
4. **验证可行性:** 每个 Task 是否带有一个明确的 `Verification Step`（如：运行某个具体的测试或检查某个端点）。

## Instructions
- 如果发现拆分过大，标记为 "⚠️ Over-scoped"。
- 如果任务描述模糊（如 "Refactor code"），强制要求细化为 "Refactor [Function Name] to handle [Specific Case]"。
- 生成一个 "Review Report" 只有在 Report 通过后，才允许进入 Execution 阶段。