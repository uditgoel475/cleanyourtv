---
name: ai-sdlc-specify
description: "Write a machine-readable feature specification from a user description"
argument-hint: "<feature description> [-svc <service> | -app <app>]"
---

# /ai-sdlc/specify

Generate a complete feature specification from the user's description.

## Steps

1. **Load repo memory** (if available):
   - Check for `.specify/memory/memory-index.md`. If present, read it plus:
     - `.specify/memory/architecture.md` — layer map, entry points
     - `.specify/memory/domain-model.md` — entities and schemas
     - `.specify/memory/patterns.md` — conventions to follow
   - If absent, proceed without memory but note: *"Run `/ai-sdlc/memorize` to pre-index this repo for richer specs."*

2. **Identify the feature type**: API/service/model, and which SWE stack (Java or Kotlin/Spring Boot, TypeScript/React, C#/.NET).
3. **Select template**:
   - SWE → `presets/swe/templates/spec-template.md` from the SDLC kit
4. **Read the constitution** at `.specify/memory/constitution.md` to ensure spec aligns with team principles.
5. **Ask clarifying questions** if any of these are missing from `$ARGUMENTS`:
   - Who is the primary user/consumer?
   - What is the measurable success criterion?
   - Are there known constraints (auth, SLA, schema)?
6. **Generate the spec** filling in all mandatory sections:
   - User Stories (at least P1, P2) with `Given/When/Then` acceptance scenarios
   - Functional Requirements (`FR-NNN`)
   - Key Entities
   - Success Criteria (`SC-NNN`)
   - Assumptions
7. **Determine spec ID**: derive a short, stable identifier from the feature title; use `SPEC-YYYY-NNN` when no clear identifier can be derived.
8. **Write files** (into the owning module when known — `$MODULE_DIR` is the
   service/app directory, e.g. `services/automation`, or `.` at repo root):
   - `$MODULE_DIR/.specify/specs/<id>/spec.md` — the spec
   - `$MODULE_DIR/.specify/specs/<id>/contracts/` — API contract if definable now
9. **Output summary**: Spec ID, path, list of open `[NEEDS CLARIFICATION]` items.

## Validation

Before finishing, check:
- No mandatory section is empty or contains only placeholder text
- Every user story has at least one `Given/When/Then`
- FR count ≥ number of user stories
- No `[NEEDS CLARIFICATION]` items remain unresolved (mark them explicitly if they must wait)

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
