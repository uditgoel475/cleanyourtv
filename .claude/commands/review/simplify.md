---
description: "Simplify code for clarity without changing behavior. Use after a feature works, during code review, or when code has accumulated unnecessary complexity."
argument-hint: "<file or directory> [-svc <service> | -app <app>]"
---

# /review/simplify

Simplify code in: **$ARGUMENTS** (or recently changed files if not specified)

## Overview

Reduce complexity while preserving exact behavior. The goal is not fewer lines — it's code that is easier to read, understand, modify, and debug. Test: "Would a new team member understand this faster than the original?"

## When to Use

- After a feature is working and tests pass but implementation feels heavy
- During code review when readability or complexity is flagged
- When you encounter deeply nested logic, long functions, or unclear names
- After merging changes that introduced duplication or inconsistency

**When NOT to use:** Code is already clean; you don't understand what it does yet; code is performance-critical; you're about to rewrite the module.

## The Five Principles

**1. Preserve Behavior Exactly** — same inputs, outputs, side effects, error behavior. If unsure, don't simplify it.

**2. Follow Project Conventions** — simplification means more consistent with the codebase, not imposing external preferences. Check `CLAUDE.md` + neighboring code first.

**3. Prefer Clarity Over Cleverness** — explicit code beats compact code when compact requires a mental pause to parse.

**4. Maintain Balance** — watch for: inlining too aggressively, combining unrelated logic, removing abstractions that exist for extensibility, optimizing for line count.

**5. Scope to What Changed** — default to simplifying recently modified code. Avoid drive-by refactors of unrelated code.

## The Simplification Process

**Step 1: Understand Before Touching (Chesterton's Fence)**

Before changing anything, answer:
- What is this code's responsibility?
- What calls it? What does it call?
- What are the edge cases and error paths?
- Are there tests that define expected behavior?
- Why might it have been written this way?

If you can't answer these, read more context first.

**Step 2: Identify Simplification Opportunities**

| Pattern | Signal | Simplification |
|---------|--------|----------------|
| Deep nesting (3+ levels) | Hard to follow control flow | Extract to guard clauses or helper functions |
| Long functions (50+ lines) | Multiple responsibilities | Split into focused functions |
| Nested ternaries | Requires mental stack | Replace with if/else or lookup objects |
| Boolean parameter flags | `doThing(true, false, true)` | Replace with options objects or separate functions |
| Generic names (`data`, `result`, `temp`) | Unclear content | Rename to describe content |
| Comments explaining "what" | `// increment counter` | Delete — the code is clear enough |
| Duplicated logic | Same 5+ lines repeated | Extract to shared function |
| Dead code | Unreachable branches, unused vars | Remove after confirming dead |

**Step 3: Apply Changes Incrementally**

Make one simplification at a time. Run tests after each change.

```
FOR EACH SIMPLIFICATION:
1. Make the change
2. Run the test suite
3. If tests pass → continue
4. If tests fail → revert and reconsider
```

**Step 4: Verify the Result**

```
COMPARE BEFORE AND AFTER:
- Is the simplified version genuinely easier to understand?
- Did you introduce patterns inconsistent with the codebase?
- Is the diff clean and reviewable?
```

## Quality Check (5-Axis)

Before marking done:

1. **Correctness** — Does it match requirements? Edge cases handled? Error paths covered?
2. **Readability** — Clear names? Straightforward flow? No clever tricks? No dead code?
3. **Architecture** — Follows existing patterns? Clean boundaries? No circular dependencies?
4. **Security** — Input validated? No secrets? Queries parameterized? Outputs encoded?
5. **Performance** — No N+1 queries? No unbounded loops? Pagination on list endpoints?

## Verification

After simplification pass:

- [ ] All existing tests pass without modification
- [ ] Build succeeds with no new warnings
- [ ] Linter/formatter passes
- [ ] Each simplification is a reviewable, incremental change
- [ ] Simplified code follows project conventions (check `CLAUDE.md`)
- [ ] No error handling was removed or weakened
- [ ] No dead code was left behind

## Red Flags

- Simplification that requires modifying tests to pass (you likely changed behavior)
- "Simplified" code that is longer and harder to follow
- Renaming things to match your preferences rather than project conventions
- Removing error handling because "it makes the code cleaner"
- Simplifying code you don't fully understand
- Batching many simplifications into one large, hard-to-review commit
