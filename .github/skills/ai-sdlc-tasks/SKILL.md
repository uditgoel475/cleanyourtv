---
name: ai-sdlc-tasks
description: "Break an approved implementation plan into ordered, executable tasks"
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /ai-sdlc/tasks

Break the approved plan for `$ARGUMENTS` into a sequenced, executable task list.

## Spec location

Resolved per the target-resolution contract above: `$SPEC_DIR = $MODULE_DIR/.specify/specs/$SPEC_ID`.

## Pre-conditions

1. Read `$SPEC_DIR/spec.md` and `$SPEC_DIR/plan.md` — both must exist.
2. Read `.specify/memory/constitution.md` for quality gate requirements.

## Task Generation Rules

- Each task must be completable in < 4 hours of focused work.
- Each task has exactly one **Definition of Done** (DoD) that is objectively verifiable.
- Tasks are ordered by dependency (no circular deps).
- Tests are tasks, not afterthoughts — write test tasks alongside implementation tasks.
- The final task is always: "Run `/review/code` and resolve all findings."

## Task Format

```text
## Task NNN — [Title]

**Depends on**: NNN (or none)
**Complexity**: XS | S | M | L
**Files**: list of files to create or modify

### Work

What to do, not how to do it.

### Definition of Done

- [ ] Specific, verifiable completion criterion
- [ ] Tests pass for this task's scope
- [ ] Type checker clean (mypy strict)
```

## Output

Write `$SPEC_DIR/tasks.md` with:

- **Summary**: total task count, estimated complexity breakdown
- **Task list**: all tasks in dependency order
- **Parallel opportunities**: tasks that can be worked concurrently (flag with `[PARALLEL]`)
- **Quality gate tasks**: coverage check, type check, spec review as explicit tasks

Aim for: 1 task per FR from the spec, plus infra tasks (migrations, contracts) and test tasks.

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
