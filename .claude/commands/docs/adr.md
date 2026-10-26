---
description: "Document architectural decisions and rationale. Use when making significant technical decisions, choosing between approaches, or adding public APIs. Documents the WHY for future engineers and agents."
argument-hint: "<decision topic or ADR title> [-svc <service> | -app <app>]"
---

# /docs/adr

Document architectural decision: **$ARGUMENTS**

## Overview

Document decisions, not just code. The most valuable documentation captures the *why* — the context, constraints, and trade-offs that led to a decision. Code shows *what* was built; documentation explains *why it was built this way* and *what alternatives were considered*.

## When to Write an ADR

- Choosing a framework, library, or major dependency
- Designing a data model or database schema
- Selecting an authentication strategy
- Deciding between backend architecture patterns (layered vs hexagonal, sync vs async)
- Choosing between API architectures
- Any decision that would be expensive to reverse
- When you find yourself explaining the same thing repeatedly

## ADR Template

Store ADRs in `docs/decisions/` with sequential numbering (`ADR-001`, `ADR-002`, etc.):

```markdown
# ADR-NNN: <Short title>

## Status
Accepted | Superseded by ADR-NNN | Deprecated

## Date
YYYY-MM-DD

## Context
What problem are we solving? What constraints exist?
- Business or technical requirement
- Existing system constraints
- Non-functional requirements (performance, security, scalability)

## Decision
<Description of the selected approach>

## Alternatives Considered

### Alternative A: <Name>
- Pros: ...
- Cons: ...
- Rejected because: <1 sentence>

### Alternative B: <Name>
- Pros: ...
- Cons: ...
- Rejected because: <1 sentence>

## Consequences
- What this enables
- What tradeoffs we accept
- What complexity this introduces
- Impact on testing / deployment / operations
```

## ADR Lifecycle

```
PROPOSED → ACCEPTED → (SUPERSEDED or DEPRECATED)
```

- **Don't delete old ADRs.** They capture historical context.
- When a decision changes, write a new ADR that references and supersedes the old one.

## Inline Documentation

### When to Comment

Comment the *why*, not the *what*:

```python
# BAD: Restates the code
# Increment counter
counter += 1

# GOOD: Explains non-obvious intent
# Rate limit uses a sliding window — reset at window boundary, not on fixed schedule,
# to prevent burst attacks at window edges
if now - window_start > WINDOW_SIZE_MS:
    counter = 0
    window_start = now
```

### When NOT to Comment

```python
# Don't comment self-explanatory code
def calculate_total(items: list[Item]) -> Decimal:
    return sum(item.price * item.quantity for item in items)

# Don't leave TODO comments for things you should just do now
# TODO: add error handling  ← Just add it

# Don't leave commented-out code ← Delete it, git has history
```

## Documentation for AI Agents

Special consideration for agent context:

- **CLAUDE.md** — Document project conventions so agents follow them
- **Spec files** — Keep specs updated so agents build the right thing
- **ADRs** — Help agents understand why past decisions were made (prevents re-deciding)
- **Inline gotchas** — Prevent agents from falling into known traps

## Verification

After documenting:

- [ ] ADRs exist for all significant architectural decisions
- [ ] `docs/decisions/` directory contains numbered ADRs
- [ ] Each ADR has Context, Decision, Alternatives Considered, and Consequences
- [ ] Status is current (not "Proposed" for accepted decisions)
- [ ] `CLAUDE.md` / project rules are current and accurate
- [ ] No commented-out code remains
- [ ] No TODO comments left for things that should be done now
