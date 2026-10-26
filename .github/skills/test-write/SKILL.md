---
name: test-write
description: "Design and implement the right mix of unit, integration, contract, and regression tests. Use after planning or during implementation when behavior changes need reliable automated verification."
argument-hint: "<SPEC-ID or file> [-svc <service> | -app <app>]"
---

# /test/write

Write tests for: **$ARGUMENTS**

## Goals

- Cover changed behavior with the smallest sufficient set of tests
- Add regression protection for defects
- Add integration or contract coverage when boundaries change
- Keep tests deterministic, readable, and maintainable

## Scope-Aware Capability Selection

Choose capabilities based on installed scope:

- **Backend scope (`be`)**: use backend-only testing capabilities (Java/Kotlin + JUnit 5, Spring test slices, integration tests). Generate spec-driven backend suites with `/test/integration`. Do not suggest Playwright/browser workflows.
- **Frontend scope (`ui`)**: use frontend/UI capabilities (component tests, browser E2E via `/test/playwright` from the `playwright-core` plugin or `/test/cypress` from the `cypress-core` plugin — whichever is installed and matches the project's existing E2E framework). Do not suggest backend-only frameworks unless backend skills are present.
- **No scope filter (`all`)**: all capabilities are available; choose only what the changed layers require.

## Test Pyramid

Invest testing effort according to the pyramid:

```
          ╱╲
         ╱  ╲         E2E Tests (~5%)
        ╱    ╲        Full user flows, real browser
       ╱──────╲
      ╱        ╲      Integration Tests (~15%)
     ╱          ╲     Component interactions, API boundaries
    ╱────────────╲
   ╱              ╲   Unit Tests (~80%)
  ╱                ╲  Pure logic, isolated, milliseconds each
 ╱──────────────────╲
```

## Decision Guide

```
Is it pure logic with no side effects?
  → Unit test

Does it cross a boundary (API, database, file system)?
  → Integration test

Is it a critical user/data flow that must work end-to-end?
  → E2E test — limit to critical paths
```

## Process

0. Load `$MODULE_DIR/.specify/memory/test-conventions.md` and `house-rules.md` when present, plus
   the testing docs that `AGENTS.md` links to. They decide the interaction API, typed helpers, and
   E2E policy. Generic guidance below is only the fallback.
1. Identify the exact behavior being introduced, changed, or fixed
2. Prefer unit tests for logic-local behavior
3. Add integration tests for component interactions (API + DB)
4. Add contract tests for APIs, schemas, or message boundaries
5. Add regression tests for bugs or incidents
6. Ensure test names explain expected behavior (descriptive, not "test_1")

## E2E Proportionality

Before adding any E2E test (Cypress/Playwright), state which part of the scenario a unit or
component test **cannot** show. Typical reasons: cross-page navigation, real browser APIs, routing
or auth guards, or a third-party widget that does not render in jsdom. If there is no such part,
the component test is the coverage. Record `E2E: not needed — <scenario> proven by <test>` in the
test plan rather than adding a browser test that reviewers will ask to remove.

## Domain-Specific Patterns

### JVM (Java / Kotlin — Spring Boot)

For backend services (for example p2-services style projects):
- Primary stack is Kotlin/Java with **JUnit 5**
- Use MockK/springmockk where Spring beans must be mocked
- Use `MockMvc` + `@SpringBootTest` / `@WebMvcTest` for REST behavior
- Prefer integration tests for persistence/message boundaries; reserve E2E browser tests for UI scope only

```kotlin
// Unit test (Kotlin + JUnit 5)
@Test
fun `rejects blank formula expression`() {
    val errors = FormulaExpression("").validate()
    assertThat(errors).contains("expression cannot be empty")
}

// Controller slice test (@WebMvcTest + MockK)
@Test
fun `create formula returns 201`() {
    mockMvc.post("/api/v1/formulas") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"name":"f1","expression":"a+b"}"""
    }.andExpect { status { isCreated() } }
}
```

For writes over a collection, test with enough rows to show the set-based path (for example 3+
rows, including one that must not change). Where the repo has a statement counter (Hibernate
statistics, datasource-proxy, EF Core interceptors), assert the statement count, so a later change
back to a per-row loop fails the test.

### TypeScript / React

The repo's conventions win over these defaults. Take them from `house-rules.md` /
`test-conventions.md`.

- **Interactions**: use `@testing-library/user-event`, not `fireEvent`, unless the repo documents
  an exception (e.g. `fireEvent.input` for controlled form fields). Match the installed major:
  v13 calls are synchronous (`userEvent.click(el)`), while v14 uses
  `const user = userEvent.setup(); await user.click(el)`.
- **No double casts in tests**: never `as unknown as T`. Use the repo's typed helpers: a
  `PartialDeep`/`PartialRootState`-style type with `satisfies PartialRootState as RootState`, a
  factory, or `satisfies Partial<T> as T`. If no helper exists, build the object with all required
  fields rather than casting.
- **Render through the shared test utilities** (providers, store, router), and read them before
  writing the first test.
- **Network**: msw handlers, or the repo's typed API mocks; never stub `fetch` ad hoc.
- **Test IDs**: from the colocated `*.testids.ts` file when the repo uses them.

```tsx
import userEvent from '@testing-library/user-event'; // v13: synchronous calls
import type { PartialRootState, RootState } from 'store';

const state = {
  appConfigReducer: { appConfig: { featuresToggles: { someFlag: true } } },
} satisfies PartialRootState as RootState;

it('opens the actions menu', () => {
  renderWithProviders(<UserCard userId="u1" />, { state }); // the repo's own render helper
  userEvent.click(screen.getByRole('button', { name: /actions/i }));
  expect(screen.getByRole('menu')).toBeVisible();
});
```

## Writing Good Tests

- **Test state, not interactions** — assert on the outcome of an operation, not which methods were called internally
- **DAMP over DRY** — in tests, Descriptive And Meaningful Phrases are better than DRY. Each test should tell a complete story
- **Prefer real implementations over mocks** — only mock at boundaries where real deps are slow or non-deterministic (external APIs, email sending)
- **One assertion per concept** — separate tests for separate behaviors
- **Arrange-Act-Assert pattern** — setup, perform action, verify outcome

## TDD Cycle

When writing tests for new behavior:

1. **RED** — write a failing test first. It must fail before implementation
2. **GREEN** — write the minimum code to make it pass
3. **REFACTOR** — clean up without changing behavior, re-run tests

## Coverage Requirements

Run the full test suite after writing tests:
- **Python (backend):** `pytest --cov --cov-fail-under=85` (minimum 85% branch coverage)
- **Frontend:** the repo's non-interactive test command with coverage (e.g.
  `yarn workspace <ws> run test:coverage <path>` or `vitest run --coverage <path>`), never a watch-mode
  command; 85% branch on changed files
- **JVM (Java/Kotlin):** `./mvnw verify` or `./gradlew check` (JaCoCo ≥ 85%)

Verify coverage hasn't decreased. Add tests to close gaps before considering the task done.

## Rules

- Avoid brittle tests tied to implementation details
- Prefer existing test conventions in the repo
- **Never declare complete if the test suite fails or coverage is below target**
- Fix failures and gaps before finishing

## Verification

Show actual command output — never claim "tests pass" without pasting evidence. Reference `/ai-sdlc/verify`.

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
