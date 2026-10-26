---
name: ai-sdlc-implement
description: "Execute the task list for a spec using test-driven development"
argument-hint: "<SPEC-ID> [task-NNN] [-svc <service> | -app <app>]"
---

# /ai-sdlc/implement

Execute implementation tasks from `$SPEC_DIR/tasks.md` using strict TDD.

## Spec location

Resolved per the target-resolution contract above: `$SPEC_DIR = $MODULE_DIR/.specify/specs/$SPEC_ID`.

## Pre-conditions

1. Read spec, plan, and tasks files for the given spec ID.
2. All three files must exist — stop and guide the user if any are missing.
3. **Load repo memory** (if available): Check for `$MODULE_DIR/.specify/memory/memory-index.md`. If present, read `patterns.md`, `test-conventions.md`, `hotspots.md`, `house-rules.md`, and `constitution.md`. `house-rules.md` is binding for production **and** test code; patterns.md is the guide for code style; hotspots.md flags files that need extra care.
4. **Read the team's own rules**: `AGENTS.md` / `CLAUDE.md` in `$MODULE_DIR` and the repo root, plus the files they link to for testing and structure. When memory and these files disagree, the repo file wins. Report the drift.
5. Identify the starting task (default: first incomplete task).

**Existing code is not automatically a precedent.** When you copy a nearby pattern, check it
against `house-rules.md` first. Legacy files often contain exactly the patterns reviewers now
reject (`fireEvent`, `as unknown as`, per-row DB writes, new files in `utils/`).

## Gate 2 — spec readiness (BLOCKING)

Checked after the spec is loaded, **before** the first TDD cycle. This is the second
readiness gate from
[ADR-006](./../../../docs/decisions/ADR-006-openspec-vs-speckit-spec-generation.md) and
[docs/SPEC_GENERATION_MODEL.md](./../../../docs/SPEC_GENERATION_MODEL.md) §5. STOP and
route the fix to the **spec-refinement phase** (re-run `/ai-sdlc/generate-spec` or edit
the spec) if any of the following holds:

- any `[NEEDS CLARIFICATION]` marker remains in the spec;
- a mandatory spec section is empty;
- an acceptance scenario has no mapped test;
- a cross-target dependency or handoff is undefined;
- **Gate 1 was bypassed and the override is declined or ungranted** — a source the spec rests on was
  never read, so its provenance is unverified. Look for `not granted` / `DECLINED` / `NOBODY`
  against the override note. Restore access and re-ingest; do not code against unverified sources.

On block, print the blocking reason(s) and the exact refinement action; do not write any
code. **Override:** proceed only with an accepted-risk note recorded in the spec, naming
who accepted it and why — and, as at Gate 1, **only the human in the current session may grant it**.
Ask, end the turn, and wait. Never record a name against a risk that person did not accept here.
Small deviations (not the conditions above) do not block.

## TDD Cycle (Per Task)

For each task, follow RED → GREEN → REFACTOR:

### RED — Write the failing test first

1. Read the task's Definition of Done.
2. Write the minimum test that captures the DoD scenario.
3. Confirm the test fails (and fails for the right reason).
4. For backend tasks: write the route test (request → response) before service logic. For frontend
   tasks: write the component/hook test with the interaction API and typed test helpers from
   `house-rules.md`.

### GREEN — Implement the minimum code to pass

1. Write the simplest code that makes the test pass.
2. Follow the plan's file map. For backend: create route, service, model/schema in order.
3. Do not implement features not covered by the current task.
4. **Writes over a collection are set-based.** Do not call save/update/delete once per element
   in a loop. Build the values first, then apply them with one joined statement per chunk (see
   `/review/perf` → *Row-by-row writes*). Per-row writes are allowed only when the plan states a
   reason.

### REFACTOR — Improve without breaking

1. Remove duplication.
2. Ensure value objects are used for domain concepts.
3. Run the target's own type, lint, and format commands from `test-conventions.md` /
   `constitution.md`, for example `./gradlew check`, `./mvnw verify`, `dotnet format`, or
   `yarn workspace <ws> run typecheck` + `yarn fix`. Use `mypy --strict` / `ruff` only for Python
   targets.

## Post-Task Checks

After every task:

- [ ] All existing tests still pass
- [ ] New tests pass
- [ ] Type check and lint clean, using the target's commands
- [ ] Coverage delta ≥ 0% (never reduce coverage)
- [ ] **House-rules scan clean.** Run each `Detect in diff` regex from `house-rules.md` against
      the added lines (`git diff -U0 <base> -- $MODULE_DIR | grep '^+' | grep -E '<regex>'`),
      tests included. Fix every hit, or cite the documented exception it falls under.
- [ ] No temporary scaffolding left behind: hard-coded feature flags, always-true selectors,
      debug overrides, `.skip`/`.only`, or commented-out code

## Completion

When all tasks are done:

1. Run full test suite with coverage report.
2. Confirm coverage ≥ 85% branch.
3. Run `/review/code` for final review.
4. Write a brief implementation summary at the bottom of `tasks.md`.

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
