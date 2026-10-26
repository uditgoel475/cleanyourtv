---
description: "Generate an implementation spec from a user-provided feature description. Clarify requirements, resolve the target, inspect the codebase, and validate the resulting spec without requiring an external ticket."
argument-hint: "<feature description> [-svc <service> | -app <app>]"
---

# /ai-sdlc/generate-spec

Generate a feature specification from the user's description: **$ARGUMENTS**

No external ticket, issue tracker, or MCP integration is required. The user may provide local files or references as optional context; do not fetch external systems unless explicitly asked.

## Route

For a contained feature or fix, generate a point-in-time spec. For a durable shared capability, cross-module contract, schema migration, or architectural decision, recommend `/ai-sdlc/change`; let the user choose.

## Workflow

### 1. Resolve the target

- Use an explicit `-svc`, `-app`, or path when provided.
- Otherwise use the current module root when clear; use the repository root for a single-project repository.
- In a monorepo, inspect the workspace structure and the user description for ownership. If multiple targets are plausible, ask one concise question and wait.
- Write under `$MODULE_DIR/.specify/specs/$SPEC_ID/`.

### 2. Load local context

If present, read `.specify/memory/memory-index.md` and the relevant memory files, `constitution.md`, repository instructions, existing specs, tests, and nearby implementation. Use only sources in the workspace or material the user supplied. Record unavailable references as assumptions or questions; do not attempt external ticket lookups.

### 3. Check existing artifacts

If the target spec directory already contains `spec.md`, `plan.md`, or `tasks.md`, do not overwrite them. Report what exists and ask whether to review, extend, or regenerate; then wait.

Check whether the requested behavior already exists in code. Summarize relevant existing behavior and the intended delta before writing.

### 4. Clarify the request

Identify the user, problem, desired outcome, observable acceptance criteria, constraints, and out-of-scope behavior. Ask one question at a time only when missing information changes the requirements, risks, or target. Do not make implementation guesses on the user's behalf.

If the description is sufficient, proceed without a clarification round. If a blocking decision remains unanswered, stop before writing the spec.

### 5. Explore and choose the workflow

Inspect the owning code, tests, and contracts needed to ground the spec. Use `/ai-sdlc/explore` for broad or uncertain integration points. Use `/ai-sdlc/brainstorm` when multiple materially different designs remain viable. A clear bug fix or straightforward feature can skip brainstorm.

### 6. Write the spec

Derive a stable kebab-case ID from the feature title. If it collides with an existing spec, append a short date or sequence suffix. Do not require an issue key.

Create `$MODULE_DIR/.specify/specs/$SPEC_ID/spec.md` with, at minimum:

- `## Objective` and `## Scope`
- `## Primary User`
- `## User Stories` with observable Given/When/Then scenarios
- `## Requirements` with functional and relevant non-functional requirements
- `## Success Criteria` with measurable outcomes
- `## Technical Approach` grounded in existing code and project conventions
- `## Files to Modify` and `## New Files`
- `## Test Plan`, mapping each acceptance scenario to the lowest useful test level
- `## Assumptions` and `## Open Questions`
- Contract, data, API, rollout, and security sections when applicable; otherwise state `N/A` with a reason

For bug fixes, include reproduction steps and the expected versus actual behavior. Carry applicable house rules into the approach without narrowing their scope. Keep requirements testable and state what must remain unchanged.

### 7. Validate and report

Before finishing, verify the required headings exist, each story has a testable scenario, success criteria are measurable, no placeholders remain, questions are recorded, and file paths match the resolved target. Validate against the repository's own spec checker when available; otherwise say that no checker was configured or available.

Report the spec ID and path, scope, acceptance-scenario count, validation result, open questions, and the next command: `/ai-sdlc/plan $MODULE_DIR/$SPEC_ID`.

$ARGUMENTS
