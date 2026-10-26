---
name: ai-sdlc-change
description: "Create an OpenSpec-style change for a durable or cross-cutting capability from the user's description, with proposal, optional design, delta specs, and tasks. No external ticket source is required."
argument-hint: "<change description> [for <target>] [as <short-name>]"
---

# /ai-sdlc/change

Create a durable capability change from the user's description: **$ARGUMENTS**

No external ticket, issue tracker, page source, or MCP integration is required. Use local project documentation and user-supplied references as context.

## When to use this

Use this workflow for durable or shared contracts, schema migrations, auth/tenancy, shared libraries, cross-module behavior, or architectural decisions. For contained features and fixes, use `/ai-sdlc/generate-spec` instead.

## Workflow

### 1. Resolve the target and change name

- Use the target named in the request; otherwise resolve it from local workspace ownership.
- In a monorepo, if ownership is ambiguous, ask and wait before writing.
- Create a short kebab-case name from the change description. Do not overwrite an existing change with the same name.
- Use the repository's established change/spec location. If it has none, use `openspec/changes/<short-name>/` for this workflow.

### 2. Ground the change

Read the applicable constitution, memory, repository instructions, current contracts/specs, implementation, and tests. Use supplied references only; do not fetch external systems. State assumptions and unresolved questions explicitly.

If the requested change is already partly implemented, identify the existing behavior and remaining delta before creating artifacts.

### 3. Write the proposal

Create `proposal.md` with:

- **Why**: problem, users affected, and desired outcome
- **What Changes**: specific additions, modifications, removals; mark breaking changes
- **Capabilities**: new or modified capability contracts
- **Affected Targets**: owning target and any dependent modules
- **Dependencies**: prerequisite code, schemas, branches, releases, or deployments
- **Impact / Risk**: security, migration, availability, compatibility, rollback
- **Open Questions**: decisions not yet resolved

### 4. Add design and delta specs when useful

Create `design.md` for cross-boundary, migration-heavy, security/performance-sensitive, or ambiguous work. Record material alternatives and the selected approach.

For each affected capability, create `specs/<capability>/spec.md` using the repository's established format. If there is no format, use:

- `## ADDED Requirements`
- `## MODIFIED Requirements`
- `## REMOVED Requirements`

Every requirement needs at least one observable scenario. For modified requirements, preserve the current contract before describing its new behavior. For removals, include a reason and migration path.

### 5. Create tasks

Write dependency-ordered `tasks.md` checkboxes covering, as applicable:

1. Design/decision acceptance
2. Shared contracts and schemas before consumers
3. Implementation by target
4. Unit, integration, and contract tests
5. Migration, rollout, rollback, and observability
6. Documentation and handoffs
7. Lint, typecheck, build, and repository quality gates
8. Final review against the proposal, design, specs, and tasks

### 6. Validate and report

Check that every change in the proposal maps to a spec and task, requirements have scenarios, handoffs are explicit, and no placeholders remain. Use repository validators when available; report when none is configured.

Summarize the change path, affected targets, artifacts, validation results, unresolved questions, and next steps. Continue with `/ai-sdlc/implement <change-path>` when approved.

$ARGUMENTS

<!-- ai-sdlc:generated -->
