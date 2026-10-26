---
description: "Execute implementation plans using grouped subagents. Groups tasks by shared files/context, dispatches one implementer + one reviewer per group. Use when plan has 5+ tasks."
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /ai-sdlc/subagent

Execute plan for **$ARGUMENTS** using grouped subagent execution.

## When to Use

- Plan has **5+ tasks** where inline execution would degrade quality
- Tasks touch **large files** (500+ lines) that bloat the context window
- You're hitting context limits or quality degrading in later tasks

## When NOT to Use

- **1-4 tasks** — inline execution is cheaper and just as good
- **Exploratory work** — unclear what needs to be done
- **Small tasks** (rename, config update) — overhead exceeds the work

## Why Group by Context

Tasks that share files belong together. One agent handles all related tasks with shared context pasted once. Token savings are significant:

```
Per-task model (6 tasks):  6 implementers + 6 reviewers = 12 dispatches
Grouped (6 tasks):         2-3 implementers + 2-3 reviewers = 4-6 dispatches
```

## Process

### Step 1: Extract All Tasks

Read the full plan from `$MODULE_DIR/.specify/specs/$SPEC_ID/plan.md` and
`$MODULE_DIR/.specify/specs/$SPEC_ID/tasks.md`, resolved per the target-resolution
contract above. Extract every task with:
- Task number and title
- What to implement
- Acceptance criteria
- Files to touch
- Dependencies on other tasks

Track all tasks upfront via TodoWrite.

### Step 2: Group Tasks by Shared Context

Build groups based on **file overlap** — tasks that touch the same files belong in the same group.

1. Build the file map: for each task, list every file it will read or modify
2. Cluster by overlap: tasks sharing at least one file go into the same group
3. Cap group size at **5 tasks** max — larger groups cause context pollution
4. Respect dependencies: tasks in sequential dependency must be in the same group

### Step 3: Prepare Shared Context Per Group

For each group, read and cache once:
- All source files that any task in the group will read or modify
- Project conventions relevant to the group's layer (from `CLAUDE.md`)
- Spec acceptance criteria relevant to the group's tasks

### Step 4: Execute Groups

Groups with no cross-group dependencies can run in parallel. Sequentially for groups with dependencies.

For each group:

#### 4a. Dispatch the Implementer

Include in the prompt:
- Scene-setting (project, where this group fits in the spec)
- All tasks in the group (numbered, in dependency order)
- All source file contents for the group (pasted once)
- Project conventions from `CLAUDE.md`
- Exact verification commands (test runner, linter, type checker)

The implementer works through tasks sequentially, running tests and lint after each.

#### 4b. Handle Implementer Response

| Status | Action |
|--------|--------|
| DONE | Proceed to reviewer |
| DONE_WITH_CONCERNS | Address correctness concerns before review; note observational ones |
| NEEDS_CONTEXT | Provide missing context, re-dispatch with additional files |
| BLOCKED | Diagnose → re-dispatch with context OR break into sub-tasks OR escalate to user |

After 3+ failed attempts → escalate to the user.

#### 4c. Dispatch the Reviewer

Reviewer gets a **fresh context** — NOT the implementer's session, only the results.

Include:
- All task specifications and acceptance criteria
- The implementer's report
- **Actual current file contents** (re-read by controller — not the implementer's claimed output)
- Project conventions from `CLAUDE.md`

**Always use `sonnet` or higher for reviewers.**

#### 4d. Handle Reviewer Response

| Verdict | Action |
|---------|--------|
| APPROVED | Mark all tasks complete, re-read files, proceed to next group |
| ISSUES FOUND | Dispatch a fix agent with specific issues. Re-dispatch reviewer. Max 2 cycles |

Never skip the reviewer. Never start the next group with open issues. Max 2 fix-review cycles per group.

### Step 5: Final Verification

After ALL groups are complete, run the project's full verification suite:
- Full test suite (all tests, not just changed files)
- Lint / type checking
- Coverage check (if enforced)

Show actual command output. Reference `/ai-sdlc/verify` — never claim "tests pass" without evidence.

## Model Selection

| Role | Model | Why |
|------|-------|-----|
| Implementer (1-2 tasks, pattern-following) | `haiku` | Cheap, sufficient for mechanical work |
| Implementer (3+ tasks, judgment needed) | `sonnet` | Needs reasoning across modules |
| Implementer (architectural decisions) | `opus` | Needs broad understanding |
| Reviewer (always) | `sonnet` or `opus` | Review requires judgment |

## Rules

- **Group by shared files.** File-overlap clustering is the single most important step.
- **Cap groups at 5 tasks.** Larger groups risk context pollution.
- **Controller provides file contents.** Paste relevant files into prompts — never make subagents read files themselves.
- **Re-read files between groups.** After Group A completes, re-read files that Group B shares before dispatching Group B's implementer.
- **Re-read files before review.** The reviewer must see what the implementer ACTUALLY wrote.
- **Never start on main without explicit user consent.** Use a branch or worktree.
