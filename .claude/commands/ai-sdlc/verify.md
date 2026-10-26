---
description: "Cross-cutting rule: no completion claims without fresh verification evidence. No 'tests pass' without showing output. Evidence precedes assertions."
---

# /ai-sdlc/verify

## The Rule

**NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE.**

Every assertion that work is done, tests pass, or code is clean must be backed by actual command output from this session. Not from memory. Not from a previous run. Fresh.

## The Sequence

1. **IDENTIFY** — which command verifies the claim?
2. **RUN** — execute it now, in full
3. **READ** — examine the complete output and exit code
4. **VERIFY** — does the output support the claim?
5. **CLAIM** — state the result WITH the evidence

Skipping any step is a violation.

## Prohibited Language

These phrases are red flags — they mean verification was skipped:

- "should pass", "should be clean"
- "probably works", "seems correct"
- "I believe this is correct"
- "tests pass" (without showing output)
- "lint is clean" (without showing output)
- "coverage is maintained" (without showing output)

## Where This Applies

| Claim | Required Evidence |
|-------|-------------------|
| "Tests pass" | Paste `pytest` or `yarn test` output showing pass count |
| "Lint is clean" | Paste `mypy`, `ruff`, or `yarn lint` output showing no errors |
| "Coverage meets target" | Paste coverage summary showing branches/functions/lines/statements |
| "Task is complete" | All of the above for the files touched |
| "Build succeeds" | Paste build output |
| "Migration works" | Paste the migration tool output (e.g. `flyway migrate`, `dotnet ef database update`) |
| "Pipeline ran" | Paste pipeline completion output |
| "Schema valid" | Paste validation output |

## Integration

This principle is referenced by:
- `/ai-sdlc/implement` — before marking any task complete
- `/ai-sdlc/subagent` — implementer report format
- `/swe/tdd` — verification section

It is not a workflow to invoke — it is a **rule to follow at every completion boundary**.

"Tests pass" is not evidence. The output of the test command is.
