# GitHub Copilot — AI SDLC Instructions

## Repository Purpose

This is a **spec-driven development kit** for software engineering (SWE) teams — Java/Spring Boot, Kotlin/Spring Boot, TypeScript/React, and C#/.NET. Specifications in `.specify/specs/` are the authoritative source of truth.

## Workflow

Before suggesting any code, check whether `.specify/specs/<id>/spec.md` and `plan.md` exist. If they don't, remind the developer to run the specify workflow first.

**Module routing:** if a change belongs to a module, write its spec under `<module>/.specify/specs/<id>/`; otherwise use the repository root. `/ai-sdlc/generate-spec` accepts a feature description directly and needs no external source integration. Resolve an explicit target from the argument; if ownership is ambiguous, ask before writing.

**Six-step process:**
1. Establish constitution → `.specify/memory/constitution.md`
2. Write spec → `.specify/specs/<id>/spec.md`
3. Write plan → `.specify/specs/<id>/plan.md`
4. Write tasks → `.specify/specs/<id>/tasks.md`
5. Implement with TDD
6. Review with `/review/code`

## SWE Suggestions

When suggesting backend code (Java or Kotlin / Spring Boot), always:

- One controller per resource, one method per HTTP operation — controllers stay thin
- Constructor injection only — never field injection (`@Autowired` on fields / `lateinit var`)
- Expose DTOs at the API boundary — never leak JPA entities through controllers
- RFC 7807 `ProblemDetail` for all error responses via a single `@RestControllerAdvice`
- Bean Validation (`@Valid`) on request DTOs; explicit typed schemas — no untyped `Object`/`Any`

When suggesting frontend code (TypeScript / React):

- Functional components only; server state via React Query, UI state via hooks
- Strict TypeScript — no `any`, no `as unknown as`

## Testing

- Real dependencies over mocks (Testcontainers for DB); never mock what you can run
- One primary assertion per test
- Coverage ≥ 85% branch on all new code

## Security

- No raw SQL string interpolation — always parameterised queries / bound parameters
- No secrets in source code — read from environment or a secrets manager

<!-- AI-SDLC-SKILLS-START -->
## Scope-Aware Capabilities

Current scope: **backend (BE) only**.

Testing capabilities to use:
- JVM backend testing with **Java/Kotlin + JUnit 5** (`.NET`: xUnit)
- Generate backend tests from a spec with `/test/integration`
- Spring test slices and integration tests (`@WebMvcTest`, `@SpringBootTest`)
- Kotlin mocking with MockK / springmockk where applicable

Do not suggest frontend/browser capabilities in this scope:
- No browser E2E test generation (Playwright or Cypress)
- No browser exploration (`/test/browser`)

## Available Agent Skills (invoke via chat or slash commands)


### Core SDD Workflow

- **`/ai-sdlc-analyze`** — Validate alignment between spec, plan, and tasks; surface gaps before implementation
- **`/ai-sdlc-archive`** — Archive a completed change: verify readiness, merge delta capability specs into the target's living specs, promote accepted ADRs, and move the change to openspec/changes/archive/. Counterpart to /ai-sdlc/change.
- **`/ai-sdlc-brainstorm`** — Socratic design discussion before spec generation. Use for ambiguous problems, multiple valid approaches, or architectural decisions. Do NOT use for clear bug fixes or tickets with unambiguous AC.
- **`/ai-sdlc-change`** — Create an OpenSpec-style change for a durable/cross-cutting capability: proposal + optional design/ADRs + delta capability specs + tasks, scoped to one target. Use for service-change/capability-change work; use /ai-sdlc/generate-spec for normal ticket-feature work.
- **`/ai-sdlc-constitution`** — Establish or update team governance constitution for this project
- **`/ai-sdlc-explore`** — Explore codebase before implementation. File discovery, architecture tracing, dependency mapping, entry point discovery. Read-only — never modifies code.
- **`/ai-sdlc-generate-spec`** — Generate a complete feature spec from a user description, clarify requirements, explore the codebase, and validate acceptance criteria
- **`/ai-sdlc-implement`** — Execute the task list for a spec using test-driven development
- **`/ai-sdlc-memorize`** — Read an existing codebase and generate structured memory files in .specify/memory/ for spec-driven development. Run once before starting SDD on a brownfield repo.
- **`/ai-sdlc-plan-impl`** — Convert an approved spec into a technical implementation plan with user approval gate. Identifies files, tests, rollout concerns, and breaks work into ordered steps before coding.
- **`/ai-sdlc-plan`** — Create a technical implementation plan from an approved spec
- **`/ai-sdlc-specify`** — Write a machine-readable feature specification from a user description
- **`/ai-sdlc-subagent`** — Execute implementation plans using grouped subagents. Groups tasks by shared files/context, dispatches one implementer + one reviewer per group. Use when plan has 5+ tasks.
- **`/ai-sdlc-tasks`** — Break an approved implementation plan into ordered, executable tasks
- **`/ai-sdlc-verify`** — Cross-cutting rule: no completion claims without fresh verification evidence. No 'tests pass' without showing output. Evidence precedes assertions.

### Software Engineering

- **`/swe-java`** — Scaffold a Java/Spring Boot feature from spec — controllers, services, JPA entities, DTOs, validation, tests

### Review & Quality

- **`/review-code`** — Perform an architecture and correctness review of changed code before merge
- **`/review-perf`** — Performance optimization with measure-first discipline. Use when performance SLAs exist, Core Web Vitals fail, profiling reveals bottlenecks, or N+1 patterns are suspected.
- **`/review-security`** — Security review covering OWASP Top 10, PII handling, and data access controls
- **`/review-simplify`** — Simplify code for clarity without changing behavior. Use after a feature works, during code review, or when code has accumulated unnecessary complexity.
- **`/review-spec`** — Validate spec completeness and alignment with team constitution before planning

### Testing

- **`/test-browser`** — Test in real browsers using Chrome DevTools MCP. Inspect DOM, capture console errors, analyze network requests, profile performance, and verify visual output at runtime.
- **`/test-integration`** — Generate backend integration & E2E tests from a spec for Spring Boot (Java/Kotlin, JUnit 5) or ASP.NET Core (.NET, xUnit). Produces controller-slice, repository, service, and full-context tests with acceptance-scenario traceability. Backend counterpart to the browser E2E plugins (/test/playwright, /test/cypress).
- **`/test-write`** — Design and implement the right mix of unit, integration, contract, and regression tests. Use after planning or during implementation when behavior changes need reliable automated verification.

### Documentation

- **`/docs-adr`** — Document architectural decisions and rationale. Use when making significant technical decisions, choosing between approaches, or adding public APIs. Documents the WHY for future engineers and agents.

### Operations

- **`/ops-ci-investigate`** — Investigate CI/CD failures without pasted links. Detects which CI systems a repo uses (Jenkins, GitLab CI, GitHub Actions, Bitbucket Pipelines), resolves the PR, branch or commit, walks every build that reported on it, reads the failed stage and job logs, classifies each failure, and hands off to the matching skill.
### Other

- **`/debug`** — Systematic root-cause debugging. Use when tests fail, builds break, behavior doesn't match expectations, or any unexpected error. 4-phase framework: reproduce → localize → reduce → fix.
<!-- AI-SDLC-SKILLS-END -->
