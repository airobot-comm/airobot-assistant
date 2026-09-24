---
name: git-commit
description: >-
  Smart Code Reviewer & Git Committer. Review current changes for quality and construct standard commit messages. Ensures artifacts are updated and forbids autonomous commits without user approval.
---

# Smart Code Reviewer & Git Committer

## Overview
This skill reviews code changes in the session, checks for common code smells (leftover debug codes, styling compliance, risk points, and plan alignment), updates artifacts (implementation plans, tasks, and walkthroughs), and creates conventional commits.

## Dependencies
None.

## Quick Start
Trigger this skill when the user asks to commit code, says "提交代码", "Review and commit", or as a final wrap-up step of a task list.

## Workflow

### Step 1: Diff Analysis (差异分析)
- Run `git diff` and `git status` to view staged and unstaged changes.
- Summarize the core logic changes: critical functions modified, new dependencies introduced, or deleted logic.

### Step 2: Quality Review (质量审查)
Review code to ensure it does not contain the following:
- **Leftovers**: Unused logs (e.g. `console.log`), `TODO`s, or temporary debug variables.
- **Compliance**: Variable naming and style aligning with project guidelines.
- **Risks**: Unhandled exceptions or null-pointer vulnerabilities.
- **Alignment**: Modifications aligning with the approved implementation plan.

### Step 3: Check and Update Artifacts (检查并更新构件)
- **IMPORTANT**: Before proposing a commit, check if artifacts (e.g. `implementation_plan.md`, `task.md`, `walkthrough.md` in the `.agent/antigravity/<session-id>/` directory) need to be updated.
- If there are changes, progress updates, or new results, update the artifact files first.
- Run `git add` to stage the updated artifacts so they are committed and archived together with the source code.

### Step 4: Commit Message Generation (生成提交信息)
Propose a Conventional Commit message in this format:
`<type>(<scope>): <description>`

- **Types**:
    - `feat`: New feature
    - `fix`: Bug fix
    - `docs`: Documentation changes
    - `style`: Formatting changes (no logic changes)
    - `refactor`: Code refactoring
    - `test`: Adding tests
    - `chore`: Auxiliary tool changes
- **Body**: Explain the "why" of the changes if they are complex.

### Step 5: Execution & Confirmation (确认与执行)
- **CRITICAL**: The agent must **NOT** autonomously run a git commit in the middle of a task or without explicit user confirmation.
- Ask the user: "Review passed. Execute commit?"
- Execute `git add .` and `git commit -m "[message]"` **ONLY** after receiving the user's explicit confirmation.

### Step 6: Sync Subtree (同步公共库)
- After the commit is successfully executed, check if any files in `libs/` were modified in the commit (`git diff-tree --no-commit-id --name-only -r HEAD | Select-String "^libs/"`).
- If changes are detected in the common library, ask the user: "Detected changes in libs. Push subtree to remote?"
- If confirmed, execute `git subtree push --prefix=libs airobot-libs main` to keep the shared repository updated.

## Output Format
### 🧐 Review Summary
- **Key Changes**: ...
- **Quality Check**: [PASS/FAIL] (Reasons if fail)
- **Suggested Commit Message**: `type(scope): message`

**Decision**: [Confirm to Commit / Needs Fix]

## Common Mistakes
- **Autocommit**: Autocommitting changes without asking the user.
- **Missing Artifacts**: Forgetting to update or commit session artifacts in `.agent/antigravity/` before commit.
- **Unconventional Messages**: Generating commit messages that violate the conventional commits format.
