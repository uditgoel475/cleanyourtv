---
description: "Read an existing codebase and generate structured memory files in .specify/memory/ for spec-driven development. Run once before starting SDD on a brownfield repo."
argument-hint: "[-svc <service> | -app <app>]"
---

# /ai-sdlc/memorize

Index the codebase at **$ARGUMENTS** and write structured memory files to
`$MODULE_DIR/.specify/memory/`.

These files are consumed automatically by `/ai-sdlc/specify`, `/ai-sdlc/generate-spec`, `/ai-sdlc/explore`, `/ai-sdlc/plan`, and `/ai-sdlc/implement` to avoid re-discovering the same architecture on every command.

> **Path resolution.** Every `.specify/memory/` path in this command is relative to `$MODULE_DIR` —
> the resolved target (an explicit `-svc`/`-app`, else the module owning the codebase being
> indexed). When no target is resolvable, `$MODULE_DIR` is the repo root (`.`). Scan and write only
> within `$MODULE_DIR` unless the task explicitly requires shared code.

---

## Pre-conditions

Parse `$ARGUMENTS`:
- **path** (optional): Target module/repo root = `$MODULE_DIR`. Defaults to the resolved target,
  else the current directory (`.`).

The kit supports a single domain — **SWE** (Java/Spring Boot, Kotlin/Spring Boot,
TypeScript/React, C#/.NET). Infer the specific stack from file extensions and
build files found.

Confirm `$MODULE_DIR/.specify/memory/` exists or create it. Do NOT overwrite existing
`constitution.md`.

---

## Phase 1 — Structural Discovery (read-only)

Explore the codebase systematically. Never edit files during this phase.

### 1a — Directory topology

```
find <path> -maxdepth 3 -type d | grep -v '__pycache__\|\.git\|node_modules\|\.venv\|dist\|build'
```

Map top-level directories to their likely roles (API layer, domain, infrastructure, tests, config, notebooks, pipelines, schemas, migrations, frontend).

### 1b — Entry points

Search for main entry points based on domain signal:

| Signal | File patterns |
|--------|--------------|
| Java / Spring Boot | `*Application.java`, `*Controller.java`, `pom.xml`, `build.gradle` |
| Kotlin / Spring Boot | `*Application.kt`, `*Controller.kt`, `build.gradle.kts` |
| C# / .NET | `Program.cs`, `Startup.cs`, `*Controller.cs`, `*.csproj` |
| TypeScript / React | `App.tsx`, `index.tsx`, `*.tsx`, pages/routes directories |
| Node/TS | `index.ts`, `server.ts`, `app.ts`, `*controller*.ts`, `*router*.ts` |
| CLI | `__main__`, `cli` entry points |

Read the top 3 most central entry-point files to understand initialization and wiring.

### 1c — Domain / business layer

Find files containing core domain logic:
- Service classes, domain models, value objects, use cases
- Look for: `class.*Service`, `class.*Repository`, `@Entity`, `@RestController`, `record.*Request`, `record.*Response`

Read representative files (2–4) from each discovered domain layer.

### 1d — Data model and schema

- JPA `@Entity` classes (Java/Kotlin), EF Core `DbContext` + entities (.NET), TypeScript interfaces/DTOs
- Flyway/Liquibase (JVM) or EF Core migration files, OpenAPI schemas
- Read the 3–5 most recently modified schema files

### 1e — Test structure

```
find <path> -name "test_*.py" -o -name "*_test.py" -o -name "*.spec.ts" -o -name "*.test.ts" | head -20
```

Identify:
- Test framework (pytest, Jest, xUnit, ScalaTest)
- Test organization pattern (mirrored dirs vs flat `tests/` folder)
- Fixture/factory pattern used
- Coverage configuration (`.coveragerc`, `pytest.ini`, `pyproject.toml [tool.pytest]`)

### 1f — Dependencies and integrations

Read: `pom.xml` / `build.gradle` / `build.gradle.kts` / `package.json` / `*.csproj`

List external dependencies grouped by category:
- Web framework, ORM, validation
- Cloud SDKs (AWS, Azure, GCP)
- Auth / security
- Messaging / queuing
- Monitoring / observability
- Test utilities

Also scan for environment variable usage and externalised config (`application.yml`,
`appsettings.json`, `.env`) — these reveal integration points.

### 1g — CI/CD and quality gates

Read: `.gitlab-ci.yml`, `Makefile`, `Jenkinsfile`, `azure-pipelines.yml`, `tox.ini`, `sonar-project.properties`

Note: lint tools, type checkers, coverage thresholds, deployment targets.

### 1h — Existing conventions (read all of them)

Team rules are scattered across several files, so read every one of these sources:

- `AGENTS.md` / `CLAUDE.md` / `CONTRIBUTING.md` / `README.md` at the repo root **and** in `$MODULE_DIR`.
- Every file those documents link to (for example `agents/testing.md`, `agents/feature-module-structure.md`).
  A rule that sits one link away still counts as a rule.
- Everything under `$MODULE_DIR/docs/` that covers testing, structure, state, or contributing.
- `.github/copilot-instructions.md`, PR templates, and lint configs (`.eslintrc*`, `biome.json`,
  `detekt.yml`, `.editorconfig`). Lint rules that ban a pattern are rules the team actually enforces.

Record each rule with its source (`path:line`). When two sources disagree, or a doc disagrees with
what recent code does, record **both** versions under *Conflicts* in `house-rules.md`. Never
silently pick one. A stale doc (for example one recommending `fireEvent` while reviewers ask for
`userEvent`) is one of the most likely things to mislead later commands.

### 1i — Measured facts (MANDATORY — run these, do not estimate)

Every number and convention below goes into a memory file that downstream commands treat as
ground truth. Each must come from a command whose output you saw in this session. Adapt the
globs to the stack; keep the counting.

> **Never put a literal backtick on the command line.** On PowerShell it is the escape character,
> so `grep 'fun ⟨backtick⟩'` silently matches nothing and reports `0` — indistinguishable from
> "this codebase doesn't use backticked names". Match it as `\x60` with `grep -P`, as below.

```bash
# Test naming — anchor to the test annotation, or private helpers inflate the camelCase count.
grep -rlP 'fun \x60' <test-root> | wc -l                        # files using backticked names
grep -rP  'fun \x60' <test-root> | wc -l                        # backticked test functions
grep -rhA1 '@Test' <test-root> | grep -cE 'fun [a-z][A-Za-z0-9]*\('   # camelCase test functions
grep -rE 'def test_|it\(|test\(' <test-root> | wc -l            # pytest / Jest styles

# File sizes — for hotspots.md "Complexity Indicators". Real line counts, not "~800+".
find <src-root> -name '*.<ext>' -type f -exec wc -l {} + | sort -rn | head -6

# Tech-debt markers — an actual count, or state zero.
grep -rE 'TODO|FIXME|HACK|XXX' <src-root> | wc -l

# Coverage gate — quote the configured number, or record [UNKNOWN].
grep -rE 'jacoco|coverage|minimum|threshold' <build-file> <ci-file> 2>/dev/null
```

Sanity-check the naming counts against the number of test functions before believing them: if
backticked and camelCase together do not roughly account for the `@Test` count, the regex is
wrong, not the codebase. Re-run rather than record the discrepancy.

**TypeScript / React — convention pairs.** Measure both sides of each pair, so a contested
convention shows up as contested instead of being decided by majority:

```bash
git grep -l -E 'fireEvent\.' -- <src> | wc -l                  # vs.
git grep -l -E 'userEvent\.' -- <src> | wc -l
git grep -l -E 'as unknown as' -- '<src>/**/*.test.ts*' | wc -l # double casts in tests
git grep -l -E 'as unknown as' -- <src> ':!*.test.ts*' | wc -l  # double casts in production code
git grep -l -E 'satisfies [A-Za-z<>]+ as ' -- <src> | wc -l     # approved typed alternative, if any
# Typed test helpers: the approved alternative to casting. Find them and count their use.
git grep -h -E 'export (type|function|const) (Partial|DeepPartial|Mock|render|create)[A-Za-z]*' -- <src>
# Installed test library majors: the API differs (e.g. user-event 13 is sync, 14 needs setup()).
grep -E '"@testing-library/(user-event|react)"|"vitest"|"jest"|"cypress"|"@playwright/test"' package.json
# Unit vs E2E ratio: shows how much the team spends on E2E.
git ls-files '<src>/**/*.test.ts*' | wc -l ; git ls-files '**/*.cy.ts' '**/*.spec.ts' | wc -l
# Legacy type-based folders inside features (new code must not land here).
git ls-files '<src>/features/**' | grep -E '/(utils|hooks|components|helpers)/' | cut -d/ -f1-4 | sort | uniq -c | sort -rn | head
# Commit scopes per feature folder: reviewers check these.
git log -200 --format=%s -- <src>/features/<feature> | grep -oE '^[a-z]+\([^)]+\)' | sort | uniq -c | sort -rn | head
```

**Backend — write shape.** Find the repo's bulk-write precedent and every per-row write inside a
loop. Later commands copy the precedent and flag the loops:

```bash
# Precedent: set-based or batched writes already in use.
git grep -n -E 'batchUpdate|unnest\(|UPDATE .* FROM \(VALUES|ExecuteUpdate|@Modifying|COPY .* FROM' -- <src>
# Not a precedent: saveAll / SaveChanges still send one statement per row (batching only cuts round trips).
# Suspects: a repository/DAO write inside a loop body (review each hit by hand).
git grep -n -E -A4 'for \(|forEach|\.map \{|foreach \(' -- <src> | grep -E '\.(save|update|delete|execute|persist|merge)\('
```

**Recency.** Majority counts show the past, not where the team is heading. Repeat the pair counts
for files added in the last 3–6 months, but first run `git rev-parse --is-shallow-repository`. In a
shallow clone, the boundary commit makes every file look newly added, so exclude the commits listed
in `.git/shallow` (or deepen the clone). If you cannot exclude them, record recency and churn as
`[UNKNOWN — shallow clone]` rather than a number.

### 1j — Review history (reviewer-enforced rules)

The rules reviewers enforce most often are written nowhere else. Merged PR comments are the best
evidence of them, and codebase counts can actively contradict them: a repo can contain 400 files
using `fireEvent` while every new PR is asked to use `userEvent`.

1. List the last ~30 merged PRs that touch `$MODULE_DIR`, using the VCS MCP tool (Bitbucket,
   GitHub, or GitLab). If it is unavailable and the optional `mcp-fallback` plugin is installed,
   use `/ops/mcp-token-fallback <vcs>`. Otherwise go to step 4.
2. Read the review comments. Keep corrections that appear in more than one PR or come from a code
   owner, for example: "use `userEvent` instead", "avoid `as unknown as`, use
   `satisfies T as T`", "no new files in `utils/`", "remove the specs folder", "no E2E for this".
3. Also scan the history for corrections: `git log -300 --format=%s%n%b -- $MODULE_DIR | grep -iE
   'instead of|replace|avoid|do not|don.t|review'`.
4. If no review history is reachable, say so. Ask the developer **once** which corrections
   reviewers repeat most, and record the answer with source `developer, <date>`.

Every harvested rule goes into `house-rules.md` with its PR/comment id as the source.

Record the command output, not an impression of it. If a command returns nothing, that is a
finding (`0`, or `[UNKNOWN — not configured]`), not a licence to infer.

---

## Phase 2 — Pattern Analysis

After reading the files above, reason about:

1. **Layering**: What architectural pattern does the code follow? (Clean Architecture, Hexagonal, MVC, Pipeline, Monolith, Microservices)
2. **Naming conventions**: Module names, class names, file names, variable names
3. **Error handling style**: Exceptions vs Result types vs None-returns
4. **Async vs sync**: Which parts use `async/await`
5. **OOP depth**: ABCs, mixins, inheritance chains vs composition
6. **Test philosophy**: Unit-heavy vs integration-heavy, mocking style
7. **Tech debt indicators**: Long functions, God classes, missing type annotations, `TODO`/`FIXME` comments
8. **Contested conventions**: whenever both sides of a pair from 1i are in real use, do not
   decide by majority. Resolve in this order and record which source decided:
   1. an explicit repo rule (1h) or lint rule;
   2. a reviewer-enforced rule (1j);
   3. the direction of recent code (1i recency);
   4. otherwise mark it `CONTESTED` and ask the developer once.

   The legacy side is still allowed where a documented exception covers it (for example
   `fireEvent.input` for controlled form fields). Record the exception and its source, not a
   blanket ban.

---

## Phase 3 — Write Memory Files

Write all files to `$MODULE_DIR/.specify/memory/`. Use the formats below exactly.

### File 1: `architecture.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
domain: swe
---

# Architecture Memory

## Layer Map

| Layer | Directory / Module | Responsibility |
|-------|--------------------|----------------|
| <layer> | `<path>` | <one-line role> |

## Entry Points

| Entry Point | File | Description |
|-------------|------|-------------|
| <name> | `<path>` | <what it does> |

## Architectural Pattern

<pattern name> — <2-3 sentence description of how the system is structured>

## Key Design Decisions (observed)

- **<decision>**: <evidence from code>
```

### File 2: `patterns.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# Code Patterns Memory

## Naming Conventions

| Thing | Convention | Example |
|-------|-----------|---------|
| Module/file | <snake_case / kebab-case / PascalCase> | `<example>` |
| Class | <> | `<example>` |
| Service/Use case | <> | `<example>` |
| Test file | <> | `<example>` |

## Framework & Library Patterns

### <Framework name>

```<language>
# Representative example from codebase
<short snippet showing the canonical pattern>
```

## Error Handling Pattern

<description with example snippet>

## Async Pattern (if applicable)

<sync / async-on-routes / fully-async — with example>

## Dependency Injection / Wiring Pattern

<description>

## When Adding a New Feature — Follow This Pattern

1. <step>
2. <step>
```

### File 3: `domain-model.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# Domain Model Memory

## Core Entities

| Entity | File | Key Fields | Relationships |
|--------|------|-----------|---------------|
| <EntityName> | `<path>` | <field1, field2> | <-> <OtherEntity> |

## Schemas / Contracts

| Schema | File | Used By |
|--------|------|---------|
| <SchemaName> | `<path>` | <which routes/pipelines> |

## State Machines (if any)

| Entity | States | Transitions |
|--------|--------|------------|

## Bounded Contexts (if identifiable)

<description or "single bounded context">
```

### File 4: `test-conventions.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# Test Conventions Memory

## Framework

- **Runner**: <pytest / Jest / xUnit / ScalaTest>
- **Coverage tool**: <coverage.py / lcov / jacoco>
- **Coverage threshold**: <N% or "not configured">

## Directory Structure

```
<actual test directory layout from the repo>
```

## Fixture / Factory Pattern

```<language>
# How tests set up test data in this repo
<representative example>
```

## Mock Strategy

<unit tests mock X, integration tests use real Y — describe what is and isn't mocked>

## Typed Test Helpers (use these instead of casting)

| Helper / type | Import | Use for | Files using it |
|---------------|--------|---------|----------------|
| `<e.g. PartialRootState, renderWithProviders, a factory>` | `<import path>` | <partial store state, render with providers> | <count from 1i> |

## Interaction and Library Versions (frontend)

- **User interaction API**: <`userEvent` / `fireEvent`, resolved per Phase 2 item 8, with source>
- **Library majors**: <e.g. user-event 13: calls are synchronous, no `setup()`>
- **Exceptions**: <e.g. `fireEvent.input` for controlled form fields, with source>

## E2E Policy

<unit : E2E file ratio from 1i>. Add E2E only for behaviour a component/unit test cannot show
(cross-page flows, real browser APIs, routing guards). <Reviewer precedent, if any.>

## Test Naming Convention

`test_<thing_being_tested>_<scenario>` — example: `<actual example from repo>`

## How to Run Tests

```bash
<actual commands from Makefile / README / pyproject.toml>
```

## When Writing New Tests — Follow This Pattern

1. <step derived from existing tests>
2. <step>
```

### File 5: `dependencies.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# Dependencies Memory

## Runtime Dependencies

| Package | Version | Category | Why Used |
|---------|---------|----------|---------|
| <name> | <version> | <web/orm/data/auth/cloud/test> | <role> |

## External Integrations

| System | How Connected | Config / Secret |
|--------|--------------|----------------|
| <system> | <SDK / REST / DB conn> | `<ENV_VAR or config key>` |

## Environment Variables Required

| Variable | Purpose | Required? |
|----------|---------|----------|
| `<VAR_NAME>` | <what it controls> | yes / no |

## Known Constraints

- <version pinning reason or compatibility note>
```

### File 6: `hotspots.md`

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# Hotspots and Caution Zones Memory

## High-Churn Files (modify carefully, test thoroughly)

Detected via: `git log --format="%f" --name-only -- <path> | sort | uniq -c | sort -rn | head -15`

| File | Commits (6 mo) | Notes |
|------|----------------|-------|
| `<path>` | <high/medium> | <why it's sensitive> |
## Complexity Indicators

| File | Lines | Recommendation |
|------|-------|---------------|
| `<path>` | <measured line count from 1i> | <what to do when touching it> |

## Tech Debt Notes

- <specific debt item with file reference>

## Safe Extension Points

| Extension Point | How to Use |
|----------------|-----------|
| `<ABC or interface>` | Subclass and register via `<registry or pattern>` |
| `<hook method>` | Override `<method_name>` in subclass |
```

### File 7: `house-rules.md`

The rules implementation and review must obey. Each rule is phrased so a later command can check
it, and comes with the approved alternative. A rule without an alternative is likely to be broken
as soon as it gets in the way of a typecheck.

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
---

# House Rules Memory

Rules apply to production **and** test code unless a row says otherwise.

## Rules

| ID | Rule | Don't | Do instead (approved alternative) | Applies to | Detect in diff | Source | Status |
|----|------|-------|-----------------------------------|------------|----------------|--------|--------|
| HR-01 | <rule> | `<forbidden pattern>` | `<alternative>`, e.g. `satisfies PartialRootState as RootState` | prod + tests | `<regex>` | `<path:line>` / PR `<id>` comment `<id>` / measured | documented \| reviewer-enforced \| contested-resolved |

## Legitimate Exceptions

| Rule | Exception | Source |
|------|-----------|--------|

## Conflicts

| Topic | Version A (source) | Version B (source) | Resolution and who decided |
|-------|--------------------|--------------------|----------------------------|

## Artifact Policy

- SDD artifacts (`.specify/`) committed to the repo: <yes / no / undecided — keep local> (source)
- Commit scope for this module: `<type>(<scope>)` (measured from `git log`, or source)

## Upstream Gaps

Reviewer-enforced rules that no repo document states. List them so the team can add them to
`AGENTS.md`. Do not edit repo-owned docs from this command.
```

### File 8: `memory-index.md` (master index)

```markdown
---
generated-by: /ai-sdlc/memorize
generated-at: <ISO date>
repo-path: <path relative to the repo root, e.g. apps/client-ui; never an absolute path>
domain: swe
language: <primary language(s)>
framework: <primary framework(s)>
---

# Repo Memory Index

Generated by `/ai-sdlc/memorize`. Consumed by all `/ai-sdlc/*` commands.

## Memory Files

| File | Purpose | Key Contents |
|------|---------|-------------|
| `architecture.md` | Layer map, entry points, pattern | Layer table, entry point table |
| `patterns.md` | Code conventions, framework patterns | Naming, snippets, how-to-add |
| `domain-model.md` | Entities, schemas, data flow | Entity table, schema table |
| `test-conventions.md` | How tests work here | Framework, fixtures, naming |
| `dependencies.md` | Runtime deps, integrations, env vars | Package table, integrations |
| `hotspots.md` | Risky files, tech debt, safe ext points | Churn table, debt notes |
| `house-rules.md` | Enforced rules with approved alternatives | Rules, exceptions, conflicts, artifact policy |
| `constitution.md` | Team governance and standards | Articles I–V |

## Quick Reference

- **Before writing or reviewing any code**: Apply `house-rules.md` (production and tests)
- **Add a new feature**: Follow the pattern in `patterns.md → When Adding a New Feature`
- **Understand data flow**: See `domain-model.md → Data Flow`
- **Write a test**: Follow `test-conventions.md → When Writing New Tests`
- **Avoid breaking things**: Check `hotspots.md → High-Churn Files` before editing
- **Check integrations**: See `dependencies.md → External Integrations`

## Refresh

Re-run `/ai-sdlc/memorize` after major refactors or when onboarding a new team member.
Last generated: <ISO date>
```

---

## Phase 4 — Git Churn Analysis

Run git log to detect high-churn files:

```bash
git -C <path> log --format="" --name-only --since="6 months ago" | \
  grep -v '^$' | sort | uniq -c | sort -rn | head -20
```

Use output to populate `hotspots.md`. If not a git repo, skip this step and note it. In a shallow
clone (`git rev-parse --is-shallow-repository` prints `true`), the boundary commit counts as a
change to every file. Exclude it, or record churn as `[UNKNOWN — shallow clone]`.

---

## Phase 5 — Validation and Summary

### 5a — Shape

- All 8 files exist in `$MODULE_DIR/.specify/memory/`
- No file contains unfilled `<placeholder>` strings
- `memory-index.md` links resolve to existing files
- No file contains an absolute path (`grep -rnE '[A-Za-z]:\\\\|/Users/|/home/' $MODULE_DIR/.specify/memory`
  finds nothing). Memory may be shared or committed, and absolute paths only work on one machine.

### 5b — Claims (BLOCKING)

Shape validation passes happily on a fabricated convention, so verify the content too. These are
the claims downstream commands act on directly, and the ones most often inferred rather than
measured:

| Claim | Re-derive with |
|---|---|
| Test-naming convention in `test-conventions.md` | Open one real test file and read a function name |
| Line counts in `hotspots.md` | The `wc -l` output from 1i |
| Churn table in `hotspots.md` | The `git log` output from Phase 4 |
| TODO/FIXME count | The `grep … \| wc -l` output from 1i |
| Coverage threshold | The configured value, or `[UNKNOWN]` |
| Every entity/table/column name | The entity file or migration — physical names differ from property names under some naming strategies |
| Every `house-rules.md` row | Its source: a doc line, lint rule, PR comment id, or the developer's answer. A rule without a source is deleted |
| Every "Do instead" alternative | It compiles: the helper or type exists (`git grep` its export) and at least one real file uses it |
| A convention picked between two patterns | The pair counts from 1i and the resolution source from Phase 2 item 8 |

If a claim cannot be traced to output you saw, delete it or mark it `[UNKNOWN]`. A memory file is
read by six other commands; a confident wrong fact is worse than an absent one.

Print summary:

```
## /ai-sdlc/memorize — Complete

Repo:    <path>
Module:  $MODULE_DIR
Domain:  swe
Stack:   <language> / <framework>

Memory files written to $MODULE_DIR/.specify/memory/:
  ✓ memory-index.md      — master index
  ✓ architecture.md      — <N> layers, <N> entry points
  ✓ patterns.md          — <N> patterns documented
  ✓ domain-model.md      — <N> entities, <N> schemas
  ✓ test-conventions.md  — <framework>, <threshold>% coverage
  ✓ dependencies.md      — <N> packages, <N> integrations
  ✓ hotspots.md          — <N> high-churn files, <N> debt notes
  ✓ house-rules.md       — <N> rules (<N> reviewer-enforced), <N> conflicts, <N> upstream gaps

Next steps:
  /ai-sdlc/constitution   — establish team governance (if not done)
  /ai-sdlc/generate-spec  — start first spec (memory is loaded automatically)
```

---

## Rules

- **Never edit source code.** This command is strictly read + write to `$MODULE_DIR/.specify/memory/`.
- **Never overwrite `constitution.md`** — it is team-owned and manually curated.
- **Never read or write outside `$MODULE_DIR`** unless the task explicitly requires shared code.
- **Measure, don't estimate.** Every count, size, and convention comes from a command in 1i or
  Phase 4 whose output you saw. `~800+ lines` and "probably camelCase" are defects, not shortcuts.
- **Prefer evidence over inference.** Every claim must be traceable to a file you read.
- **Flag unknowns explicitly.** Write `[UNKNOWN — could not determine from codebase]` rather than guessing.
- **Majority is not a convention.** A legacy pattern can outnumber the one reviewers now require.
  When both are in use, resolve the pair with Phase 2 item 8, never by which count is bigger.
- **Every rule comes with its alternative, and applies to tests too.** "No `as unknown as`" with
  no named replacement, or scoped only to "boundaries", gets broken in test code first.
- **Refresh from review.** When a PR review corrects something memory got wrong or left out, add
  the rule to `house-rules.md` with the PR comment id before the next feature starts. Each
  correction should only have to happen once.
- **Hotspot churn requires git history.** Do not invent churn data; run the git command or skip the section.
- **A delegated report is a lead, not a finding.** If a subagent gathered the facts, re-derive every
  number and every convention yourself before writing it down — the same rule
  [`/ai-sdlc/subagent`](./subagent.md) applies to implementer output, and
  [`/ai-sdlc/verify`](./verify.md) to completion claims. Summaries smooth measurements into
  impressions; that is exactly what memory must not contain.
- **Keep memory files concise.** Each file should be scannable in 60 seconds. Use tables, not prose paragraphs.

---

## How Downstream Commands Use This Memory

| Command | Memory files read |
|---------|------------------|
| `/ai-sdlc/specify` | `constitution.md`, `architecture.md`, `patterns.md`, `domain-model.md` |
| `/ai-sdlc/generate-spec` | All 8 files (full context before spec generation) |
| `/ai-sdlc/explore` | `architecture.md`, `domain-model.md`, `hotspots.md` |
| `/ai-sdlc/plan` | `architecture.md`, `patterns.md`, `dependencies.md`, `house-rules.md` |
| `/ai-sdlc/implement` | `patterns.md`, `test-conventions.md`, `hotspots.md`, `house-rules.md` |
| `/test/write` | `test-conventions.md`, `house-rules.md` |
| `/review/code` | `patterns.md`, `hotspots.md`, `house-rules.md`, `constitution.md` |
| `/review/security` | `dependencies.md`, `domain-model.md`, `constitution.md` |

These commands should check for `.specify/memory/memory-index.md` at startup and load relevant files before proceeding. If the file is absent, prompt: *"Run `/ai-sdlc/memorize` to index this repo before generating specs."*
