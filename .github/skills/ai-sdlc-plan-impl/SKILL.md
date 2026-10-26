---
name: ai-sdlc-plan-impl
description: "Convert an approved spec into a technical implementation plan with user approval gate. Identifies files, tests, rollout concerns, and breaks work into ordered steps before coding."
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /ai-sdlc/plan-impl

Convert approved spec into a technical plan: **$ARGUMENTS**

Use this skill after the problem is understood and before coding begins.

## Goals

- Turn a spec into a safe technical approach
- Break work into ordered tasks
- Minimize risk through small, reversible changes
- Identify testing, rollout, compatibility, and observability needs

## Inputs

- Approved spec at `$MODULE_DIR/.specify/specs/$SPEC_ID/spec.md`, resolved per the
  target-resolution contract above.
- Codebase exploration findings — if spec was generated via `/ai-sdlc/generate-spec` (which includes Pre-Step C: explore-codebase), the spec's Technical Approach already contains these findings. Only re-run `/ai-sdlc/explore` if the spec was written manually or the codebase has changed significantly.
- Constitution: `.specify/memory/constitution.md`
- House rules: `.specify/memory/house-rules.md`, when present. Every step that touches code names the
  rules it must follow.

## Process

**Step 1:** Restate the technical objective.

**Step 2:** List impacted modules, contracts, tests, data models, and operational surfaces.

**Step 3:** Choose the simplest viable design that fits existing architecture. For every write over
a collection, choose a set-based statement (staged values + one joined statement per chunk) or
state why rows must be written one by one (`/review/perf` → *Row-by-row writes*).

**Step 4:** Break work into ordered steps. Mark any steps that can be parallelized.

**Step 5:** Design the test strategy — for each task, determine the right mix of test types:
- Unit tests for pure logic and data transforms
- Integration tests for API boundaries and DB interactions
- Component tests for UI behavior (SWE)
- E2E only for what a component/unit test cannot show; say what that is
- Map every acceptance scenario to an implementation task and executable test

**Step 6:** Include a lint step in every plan — after any code change, the plan must include the project's lint and type-check commands. Fix all violations before marking the task done.

**Step 7:** **Present the plan and wait for explicit user approval.** Do not write any code until the user responds with clear approval. This is a hard stop — never infer approval from silence or partial acknowledgement.

## Conditional Agent Reviews

Before finalizing, check if either applies:

### Legacy Impact Assessment (conditional)

**Agent:** `legacy-impact-assessor`

**Run when** the change touches legacy code, shared modules, old APIs, or code with unclear ownership.

Dispatch with the list of files the plan will modify. Incorporate findings into the plan's risks section.

### Migration/Rollout Review (conditional)

**Agent:** `migration-rollout-reviewer`

**Run when** the plan includes database schema changes, API contract changes, migration/backfill jobs, or multi-service deployment sequencing.

Dispatch with the migration/API change details. Incorporate findings into rollout notes.

If neither condition applies, skip both and proceed to plan approval.

## Output Format

```markdown
## Technical Plan: <SPEC-ID>

### Objective
[1-2 sentence restatement of what we're building]

### Impacted Files
| File | Role | Change Type |
|------|------|-------------|

### Ordered Implementation Steps
1. **Step title** (files: X, Y)
   - What: [description]
   - Test: [unit/integration/e2e, key scenario]
   - Lint: [command]
   - Parallel with: [N/A or step N]

2. ...

### Test Plan
| Layer | Type | Key Scenarios |
|-------|------|---------------|

### Risks and Mitigations
| Risk | Mitigation |
|------|-----------|

### Rollout Notes
[Migration steps, deployment sequence, feature flags]

---
**Awaiting user approval before coding begins.**
```

## Rules

- Prefer additive and incremental changes over broad rewrites
- Respect architecture boundaries from the constitution
- Highlight unknowns early instead of hiding them
- **Never begin implementation without explicit user approval of the plan**

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
