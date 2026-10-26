---
name: review-spec
description: "Validate spec completeness and alignment with team constitution before planning"
argument-hint: "<SPEC-ID> [-svc <service> | -app <app>]"
---

# /review/spec

Resolved per the target-resolution contract above:
`$SPEC_DIR = $MODULE_DIR/.specify/specs/$SPEC_ID`.

Validate that `$SPEC_DIR/spec.md` is complete and constitution-compliant.

## Completeness Checks

### Mandatory Sections

Gate on the required-heading set in
[`/ai-sdlc/generate-spec` §5d](./../ai-sdlc/generate-spec.md) — the two must never
disagree. A section that does not apply must still be present with an `N/A — <reason>` body.

- [ ] Every required heading from §5d is present
- [ ] User Stories section exists with at least one P1 story
- [ ] Each user story has at least one `Given/When/Then` scenario
- [ ] `Requirements` section has at least 3 `FR-NNN` items
- [ ] `Success Criteria` section has at least 1 measurable `SC-NNN`
- [ ] `Assumptions` section is non-empty

### SWE Completeness (if SWE spec)

- [ ] `Primary User` names the user type
- [ ] `API Surface` describes method, path, and auth requirement (or `N/A — <reason>`)
- [ ] `Key Entities` lists the data model entities
- [ ] Error states in acceptance scenarios (not just happy path)

### Quality

- [ ] No `[NEEDS CLARIFICATION]` items remaining open
- [ ] No placeholder text (`[LIKE THIS]`) left unfilled
- [ ] Success criteria are measurable, not vague ("system is fast" is invalid)
- [ ] Acceptance scenarios are independently testable
- [ ] Every `§Section` reference in `checklist.md` resolves to a heading in `spec.md`
- [ ] Every open question names a decision owner

## Constitution Alignment

- [ ] Spec reflects the user's stated goal and scope
- [ ] Assumptions and out-of-scope behavior are explicit
- [ ] Spec ID is stable and matches the spec folder

## Output Format

```markdown
## Spec Review: $ARGUMENTS

### Status: READY_TO_PLAN | INCOMPLETE | INVALID

### Missing / Invalid Items
- User Story 2 has no Given/When/Then
- Data model entities missing from Key Entities section

### Warnings
- SC-001 is not measurable ("improve performance" → needs a number)

### Recommendation
Fix the 2 blocking items above, then run /ai-sdlc/plan
```

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
