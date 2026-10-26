---
name: ai-sdlc-constitution
description: "Establish or update team governance constitution for this project"
argument-hint: "[-svc <service> | -app <app>]"
---

# /ai-sdlc/constitution

Adapt the embedded constitution template below for the **resolved target** and write it to
`$MODULE_DIR/.specify/memory/constitution.md`.

`$MODULE_DIR` is resolved per the target-resolution contract (an explicit `-svc`/`-app`, else the
module owning the file under discussion). When no target is resolvable, `$MODULE_DIR` is the repo
root (`.`).

## Steps

1. Use the **Constitution Template** section below as the master template. It is authoritative.
   The kit repository's own `.specify/constitution.md` governs the kit itself; it is not a template
   for other projects.
2. Read the team's own rules before writing anything: `AGENTS.md` / `CLAUDE.md` in `$MODULE_DIR`
   and the repo root, **every file they link to**, `$MODULE_DIR/docs/`, and — when
   `/ai-sdlc/memorize` has run — `$MODULE_DIR/.specify/memory/house-rules.md` and
   `test-conventions.md`.
3. Identify the target's implementation stack (Java/Spring Boot, Kotlin/Spring Boot,
   TypeScript/React, C#/.NET, or a mix). Keep only the stack sections that apply and delete the rest.
4. Generate a tailored constitution for **this target**:
   - **Every standard cites its source**, e.g. `(AGENTS.md:80)`, `(house-rules HR-03)`, or
     `(kit default)`. A standard with no source is invented, so either find a source or drop it.
   - **Repo-owned rules take precedence.** The constitution adds SDD process on top of `AGENTS.md`
     and the team's docs. It never overrides them. Copy their rules faithfully, including scope:
     do not narrow "never X" to "never X at boundaries".
   - **Rules apply to test code too**, unless the source explicitly exempts tests. For each
     prohibition, name the approved alternative from `house-rules.md`, e.g. "no `as unknown as`;
     use `satisfies PartialRootState as RootState`".
   - **Kit-only process rules are proposals.** Committing `.specify/` to the repo, requiring a spec
     per branch, and branch-naming schemes are team decisions. Mark them `Proposed — not adopted`
     unless a repo doc or the developer confirms them.
   - Quality gate thresholds appropriate for the project maturity, taken from CI config when one
     exists.
5. **Ask before writing, in ONE question:** (a) is `.specify/` committed to the repo or kept local
   until the team agrees, and (b) are the `Proposed` rules adopted? End the turn and wait for the
   answer, then record it in Article II.
6. **Do not fabricate ratification.** Set `Status: DRAFT — not ratified` and leave `Ratified`
   empty unless the human in this session says the team has ratified it. A ratification date
   nobody gave is a false governance record, the same rule as Gate 1 overrides.
7. Write the file to `$MODULE_DIR/.specify/memory/constitution.md`. Use repo-relative paths only;
   never absolute paths.
8. Confirm the file was written. Summarise the principles, which sources each came from, and any
   repo rule you could not place (so the developer can check nothing was dropped).

## Output

File: `$MODULE_DIR/.specify/memory/constitution.md`

The constitution must include all five articles from the template:
- Article I: Core Principles (P1–P5)
- Article II: Development Workflow (six-step SDD process)
- Article III: Domain Standards
- Article IV: Quality Gates with thresholds
- Article V: Amendment Process

## Constitution Template

```markdown
# <Target> Team Constitution

**Version**: 1.0.0
**Status**: DRAFT — not ratified
**Ratified**: <YYYY-MM-DD, only when the team has ratified it>
**Relationship to repo rules**: Adds the SDD process on top of `AGENTS.md` and the team's docs.
When they conflict, the repo-owned rule wins; raise the conflict with the team instead of
overriding it here.

---

## Article I — Core Principles

### P1: Specification-First Development
No production code is written without a corresponding spec. Every feature begins with a `spec.md`
that defines user stories, acceptance criteria, and success metrics before implementation starts.
**Enforcement**: every change to this target has a `.specify/specs/<id>/spec.md`. Whether that
folder is committed or kept local follows the artifact policy in Article II.

### P2: Contracts Define Boundaries
API contracts, data schemas, and interfaces are explicit, versioned artifacts in
`.specify/specs/<id>/contracts/`. Breaking a contract requires a new spec with a migration plan.

### P3: Tests Are the Spec Made Executable
Every `Given/When/Then` acceptance scenario maps to at least one automated test. A spec is not
implemented until all its acceptance scenarios pass. **Minimum coverage**: 85% branch.

### P4: Schema Over Convention
- **Java/Kotlin**: DTO boundary (MapStruct / extension functions); JPA entities never exposed via REST
- **C#/.NET**: FluentValidation on DTOs; EF Core entities never exposed via controllers
- **TypeScript/React**: strict `tsconfig`, typed API client; no `any` and no double cast
  (`as unknown as`) anywhere, **including tests**. Use the repo's typed test helpers instead
  (see `house-rules.md`, e.g. `satisfies PartialRootState as RootState`)

### P5: OOP / Module Contracts Are Sacred
Base classes and shared interfaces define the extension contract. Subclasses may override methods
but must never weaken post-conditions (Liskov Substitution). Removing a contract method requires an
amendment.

---

## Article II — Development Workflow

### W1: Six-Step SDD Process
| Step | Artifact | Tool |
| --- | --- | --- |
| 1. Constitution | `.specify/memory/constitution.md` | `/ai-sdlc/constitution` |
| 2. Specify | `.specify/specs/<id>/spec.md` | `/ai-sdlc/generate-spec` |
| 3. Plan | `.specify/specs/<id>/plan.md` | `/ai-sdlc/plan` |
| 4. Tasks | `.specify/specs/<id>/tasks.md` | `/ai-sdlc/tasks` |
| 5. Implement | Working code + tests | `/ai-sdlc/implement` |
| 6. Review | Review checklist | `/review/code` |

### W2: User-Provided Feature Input
Start from the user's feature description and acceptance criteria. No external ticket or issue-tracker
integration is required. Derive a stable spec ID from the title; use `SPEC-YYYY-NNN` when needed.

### W3: Branch and PR Naming
Use the repo's documented convention (cite it), e.g. `<issue-id>_short-description`, and the
commit scope measured in `house-rules.md`. Without a documented convention, mark this
`Proposed — not adopted`:
- Feature branches: `feature/<spec-id>-short-description`
- Hotfixes: `hotfix/<issue-id>-description`

### W4: Artifact Policy
`.specify/` is <committed | kept local until the team agrees> (decided by <who>, <date>).
If kept local, PRs contain code only, and specs never include absolute paths or edit history.

---

## Article III — Domain Standards

Keep the sections for this target's stack and delete the rest. Each item cites its source.

### Backend Standards (Java / Kotlin / .NET)
1. One controller/handler per resource, one method per HTTP operation
2. Constructor injection for all dependencies (no field injection)
3. Value objects over primitives for domain concepts
4. Versioned migrations for all schema changes — no DDL outside migrations
5. Draft/publish state machine for mutable domain entities
6. **Set-based writes.** Never issue one UPDATE/INSERT/DELETE per element of a collection. Stage
   the values (a `VALUES` list, `unnest` arrays, or a temp table) and apply them in one joined
   statement per chunk. Per-row writes need a stated reason in the plan, such as per-row domain
   events, optimistic locking, or per-row error isolation.

### Frontend Standards (TypeScript / React)
1. Follow the repo's folder rule (cite it). Do not add new files to legacy type-based folders
   (`utils/`, `hooks/`, `components/`) inside a feature, even when the folder already exists.
2. Server state through the repo's data layer (e.g. `redux-queryable`, RTK Query, React Query),
   as cited, never a new one.
3. User-visible strings from the source-language locale; UI components from the design system.

### Test Standards
1. Interaction API as resolved in `house-rules.md` (e.g. `userEvent`, matching the installed
   major version), with its documented exceptions.
2. Typed test helpers instead of casts, in every test file.
3. Prove each acceptance scenario at the lowest level that can show it. Add E2E only for behaviour
   a component or unit test cannot show (cross-page flows, real browser APIs, routing guards).

---

## Article IV — Quality Gates
| Gate | Threshold | When Applied |
| --- | --- | --- |
| Test coverage | ≥85% branch | Every PR |
| Type checking | Compiler/type-check clean, 0 errors | Every PR |
| Spec completeness | All `[NEEDS CLARIFICATION]` resolved | Before plan step |
| Security review | `/review/security` clean | For auth/PII changes |

---

## Article V — Amendment Process
Amendments require:
1. A spec (`spec.md`) describing the proposed change and rationale
2. Review by at least two team members
3. Update to this document with version bump and ratification date
```

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
