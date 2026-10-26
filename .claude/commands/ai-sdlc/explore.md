---
description: "Explore codebase before implementation. File discovery, architecture tracing, dependency mapping, entry point discovery. Read-only — never modifies code."
argument-hint: "<feature description or TICKET-ID> [-svc <service> | -app <app>]"
---

# /ai-sdlc/explore

Understand the codebase for: **$ARGUMENTS**

Use this skill when you need to understand the project before proposing or implementing changes.

## Goals

- Identify relevant files, modules, interfaces, tests, configs, and runtime entry points
- Trace how control and data move through the system
- Surface assumptions, uncertainties, and hidden dependencies
- Recommend where a change should likely be made

## Process

1. **Load repo memory** (if available): Check for `.specify/memory/memory-index.md`. If present, read `architecture.md`, `domain-model.md`, and `hotspots.md` before exploring. Skip live file scans for anything already covered — memory is ground truth until the code contradicts it.
2. Start from the user request, spec, ticket, or failing behavior
3. Identify likely entry points, routers, controllers, services, jobs, handlers, schemas, and test files
4. Trace inward and outward dependencies
5. Note existing patterns that should be followed — not inventing new ones
6. Summarize findings before proposing implementation

## Output Format

Present a structured findings summary:

```
## Codebase Exploration: <topic>

### Relevant Files and Directories
- `path/to/file.py` — Role/responsibility
- `path/to/dir/` — Description

### Current Flow of Control/Data
[Prose description of how data flows through the system]

### Existing Design Pattern to Follow
[Specific pattern with example file reference]

### Domain Rules and Constraints Discovered
- Rule 1
- Constraint 2

### Unknowns or Ambiguities
- [ ] Unknown 1 — needs investigation
- [ ] Ambiguity 2 — needs clarification

### Suggested Next Step
- Proceed to `/ai-sdlc/brainstorm $ARGUMENTS` (complex)
- OR proceed to `/ai-sdlc/generate-spec $ARGUMENTS` (simple)
```

## Rules

- **Do not edit files.** This is a read-only exploration skill.
- **Do not guess architecture without evidence** from the codebase.
- Prefer concise evidence-based summaries over speculation.
- Surface unknowns explicitly — don't silently fill ambiguity.

## Use Cases

- "Find where the pipeline execution logic is implemented"
- "Map the request flow for the formula evaluation endpoint"
- "Identify all places impacted by adding a new data quality rule"
- "Understand how the service layer handles schema validation"
- "Trace how a spec.md maps to generated code"
