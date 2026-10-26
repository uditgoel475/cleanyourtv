---
description: Validate alignment between spec, plan, and tasks; surface gaps before implementation
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /ai-sdlc/analyze

Perform a cross-artifact consistency check on spec → plan → tasks for `$ARGUMENTS`.

## Spec location

Resolved per the target-resolution contract above: `$SPEC_DIR = $MODULE_DIR/.specify/specs/$SPEC_ID`.

## What to Check

### Spec Completeness

- All user stories have at least one `Given/When/Then`
- No `[NEEDS CLARIFICATION]` items remain
- Every `FR-NNN` has a measurable success criterion

### Plan Coverage

- Every `FR-NNN` from the spec appears in the plan's file map
- API/schema contracts exist in `$SPEC_DIR/contracts/`
- Risk register has at least 3 entries
- Dependency list is complete

### Task Completeness

- Every `FR-NNN` has at least one task
- Every task has a Definition of Done
- Test tasks exist for all implementation tasks
- Quality gate tasks (coverage, type check) are present
- No circular dependencies in task ordering

### OOP Contract Alignment (Framework)

- For SWE tasks: spec model entities map to explicit `BaseSpec` subclass
- If `framework/` is extended: no ABC methods removed or weakened

## Output

Produce a structured report:

```text
## Spec Analysis: $ARGUMENTS

### Status: READY | NEEDS WORK | BLOCKED

### Gaps Found
- FR-003 has no corresponding task
- api-contract.md missing from contracts/

### Warnings
- Task 005 depends on Task 007 (ordering issue)

### Recommendations
- Add validation task for null check on request.customer_id
```

If status is READY, confirm the user may proceed to `/ai-sdlc/implement`.
If status is NEEDS WORK or BLOCKED, list exactly what must be fixed and in which file.

$ARGUMENTS
