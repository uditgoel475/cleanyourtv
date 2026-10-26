---
name: ai-sdlc-brainstorm
description: "Socratic design discussion before spec generation. Use for ambiguous problems, multiple valid approaches, or architectural decisions. Do NOT use for clear bug fixes or feature descriptions with unambiguous acceptance criteria."
argument-hint: "<topic or feature description> [-svc <service> | -app <app>]"
---

# /ai-sdlc/brainstorm

Explore the design space for: **$ARGUMENTS**

## Hard Gate

No spec generation, no code, no scaffolding until a design direction is chosen and validated. Every complex feature goes through this process.

## When to Use

- Complex features with multiple valid approaches
- Ambiguous requirements where "what to build" is unclear
- Architectural decisions (new data model, new service boundary, new UI pattern, new pipeline design)
- Features touching 3+ layers (DB + backend + frontend, or source + transform + target)
- When the user says "I'm not sure how to approach this"

## When NOT to Use

- Bug fixes with clear reproduction steps
- Small tweaks (add a field, rename a column)
- Feature descriptions with clear acceptance criteria and a single obvious approach
- Pure refactors where the target architecture is already decided

## Process

### Step 1: Understand the Problem

Ask the user to describe what they're trying to achieve. Do NOT ask about implementation yet.

Focus on:
- What problem does this solve? Who benefits?
- What does success look like from the user's perspective?
- What happens today without this feature?

Use the user's description as the starting point. No ticketing-system integration is required; ask for any missing context directly.

**One question at a time. Never batch questions.**

### Step 2: Explore Constraints

Before proposing solutions, understand the boundaries:
- What existing patterns in the codebase are relevant? (Use `/ai-sdlc/explore` if needed)
- Are there performance, security, or compatibility constraints?
- What's out of scope? What should this explicitly NOT do?
- Is there a timeline or dependency that constrains the approach?

**One question at a time. Never batch questions.**

### Step 3: Propose Approaches

Present 2-3 concrete approaches. For each:

| Approach | Description | Pros | Cons | Effort |
|----------|-------------|------|------|--------|
| A: ... | ... | ... | ... | Low/Med/High |
| B: ... | ... | ... | ... | Low/Med/High |
| C: ... | ... | ... | ... | Low/Med/High |

Include a **Recommended** tag on the approach you'd pick, with a 1-sentence reason.

Do NOT present more than 3. Filter to the most viable.

### Step 4: Converge

Ask the user which approach they prefer, or if they want to combine elements. Continue one-question-at-a-time until the direction is clear.

Confirm: "We're going with Approach [X]: [one-line summary]. Correct?"

### Step 5: Save Design Document

Write a brief design doc to `$MODULE_DIR/.specify/specs/$SPEC_ID/designs/YYYY-MM-DD-<topic>.md`
using ADR format (`$MODULE_DIR` is the owning service/app directory when known —
e.g. `services/automation` — or `.` when brainstorming at the repo root):

```markdown
# Design: <Topic>

## Status
Accepted

## Context
[What problem are we solving? What constraints exist? 2-3 sentences]

## Decision
[Description of the selected approach]

## Alternatives Considered
- **Approach B:** [Why rejected — 1 sentence]
- **Approach C:** [Why rejected — 1 sentence]

## Consequences
- [What this enables]
- [What tradeoffs we accept]

## Open Questions
- [Anything deferred to spec generation]
```

Keep it under 1 page. This is a decision record, not a spec.

## Terminal State

After the design doc is saved and the user confirms:

> "Design approved. Proceed with `/ai-sdlc/generate-spec $ARGUMENTS` using this design as context."

The `/ai-sdlc/generate-spec` skill will read the design doc and use the chosen approach for the technical approach section.

## Rules

- **One question at a time.** Never present 3 questions in one message. Ask, wait, process, ask the next.
- **Concrete options, not open-ended questions.** Instead of "How should we handle errors?", ask "Should validation errors return inline messages (A) or a toast notification (B)?"
- **No premature implementation detail.** Proposals describe *what* each approach does, not *how* to code it.
- **Respect the user's time.** If the user signals the direction is obvious ("just do X"), skip to Step 5.
- **Design doc is brief.** Under 1 page. It captures the *decision*, not the full design.

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
