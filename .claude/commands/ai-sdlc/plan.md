---
description: Create a technical implementation plan from an approved spec
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>] (e.g. add-home-controls -svc services/launcher)"
---

# /ai-sdlc/plan

Generate a technical implementation plan for the spec identified by `$ARGUMENTS`.

## Spec location

Resolved per the target-resolution contract above: `$SPEC_DIR = $MODULE_DIR/.specify/specs/$SPEC_ID`.

## Pre-conditions

1. Read `$SPEC_DIR/spec.md` — must exist and have no unresolved `[NEEDS CLARIFICATION]`.
2. **Gate 1 must be settled.** If the spec records a Gate 1 override that is declined or ungranted
   (`DECLINED`, `not granted`, `NOBODY`), STOP — the spec rests on a source that was never read.
   Restore access, re-ingest, and re-run `/ai-sdlc/generate-spec` before planning.
3. Read `.specify/memory/constitution.md` — plan must honour all constitutional standards.
4. **Load repo memory** (if available): Check for `.specify/memory/memory-index.md`. If present, read `architecture.md`, `patterns.md`, `dependencies.md`, and `house-rules.md` to inform the file map and technical approach.
5. If spec does not exist, stop and instruct the user to run `/ai-sdlc/specify` first.

## Steps

### SWE Plan

1. Identify affected layers: routes, services, models, schemas, migrations, tests.
2. Map each `FR-NNN` to a specific file or function.
3. Define the API contract (request/response schemas) in `$SPEC_DIR/contracts/api-contract.md`.
4. List database migrations required (new tables, columns, indexes) — Flyway/Liquibase (JVM) or EF Core (.NET).
5. Identify external dependencies (auth, blob storage, external APIs).
6. **Data-access shape**: for every write over a collection, name the set-based statement, its
   staging form, and chunk size. Otherwise state why rows must be written one by one
   (`/review/perf` → *Row-by-row writes*).
7. **File placement**: put every new file where the repo's folder rule (`house-rules.md` /
   `AGENTS.md`) says, never in a legacy type-based folder just because it exists.
8. **Test level per scenario**: the lowest level that proves it. Any E2E entry states what a
   component/unit test cannot show.
9. Estimate complexity: S / M / L / XL.

## Output

Write `$SPEC_DIR/plan.md` using the plan-relevant sections of
`presets/swe/templates/spec-template.md` as the base, containing:

- **Technical approach**: chosen patterns and why
- **File map**: each file to be created/modified with its responsibility
- **Data/API contract**: link to contract files
- **Dependencies**: internal and external
- **Risk register**: top 3 risks with mitigation
- **Complexity estimate**: S / M / L / XL with justification

$ARGUMENTS
