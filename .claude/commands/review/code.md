---
description: Perform an architecture and correctness review of changed code before merge
argument-hint: "[<SPEC-ID> or file paths] [-svc <service> | -app <app>]"
---

# /review/code

Perform a structured code review of the current changes.

## Scope

Review the **whole branch diff** against its base: `git diff --name-only $(git merge-base HEAD
origin/<default-branch>)...HEAD`, plus any uncommitted changes. Test files, fixtures, and config
are in scope too. Reviewers comment on them as much as on production code.

If `$ARGUMENTS` is a spec ID (optionally module-qualified as `services/<module>/<SPEC_ID>`), also
cross-check the diff against `$MODULE_DIR/.specify/specs/$SPEC_ID/plan.md`. Report files the plan
lists but the diff lacks, and files the diff touches that the plan never mentions.

## Load the rules first

Read `$MODULE_DIR/.specify/memory/house-rules.md`, `patterns.md`, `hotspots.md`, and
`constitution.md` when present, plus `AGENTS.md` / `CLAUDE.md` in `$MODULE_DIR` and the repo root
and the files they link to. When these disagree, the repo-owned file wins.

**House-rules scan (BLOCKING).** For every rule with a `Detect in diff` regex, grep the added lines
(`git diff -U0 <base>...HEAD -- $MODULE_DIR | grep '^+'`), tests included. Every hit is a
**Critical** finding unless it falls under a listed exception. If `house-rules.md` does not exist,
say so in the output. In that case the review cannot vouch for team conventions, so recommend
`/ai-sdlc/memorize`.

## Review Checklist

Use the sections that match the changed stack; mark the others N/A.

### Architecture (backend)

- [ ] Route files contain only routing logic (no business logic)
- [ ] Business logic is in `services/`, not `models/` or `routes/`
- [ ] ORM queries are not in route handlers or schemas
- [ ] `framework/base/` ABCs are not broken or weakened

### Architecture (frontend)

- [ ] New files follow the repo's folder rule; nothing new lands in a legacy type-based folder
      (`utils/`, `hooks/`, `components/`) inside a feature, even if that folder already exists
- [ ] Server state uses the repo's existing data layer; UI components come from the design system
- [ ] Every user-visible string comes from the source-language locale file
- [ ] No barrel files, no cross-feature deep imports

### Correctness

- [ ] All acceptance scenarios from the spec have a corresponding test
- [ ] Edge cases from spec `Edge Cases` section are tested
- [ ] Error paths return appropriate HTTP status codes (400/404/409/422/500)
- [ ] Database transactions are committed or rolled back correctly
- [ ] No N+1 query problems in list endpoints
- [ ] **No row-by-row writes**: no save/update/delete/execute inside a loop over a collection.
      Values are staged and applied in one joined statement per chunk (`/review/perf` →
      *Row-by-row writes*), or the plan states why each row must be written alone
- [ ] No temporary scaffolding: hard-coded or always-true feature flags, debug overrides,
      `.skip`/`.only`, commented-out code

### Type Safety

- [ ] The target's type checker passes with zero errors (`tsc`/`typecheck`, Kotlin/Java compiler,
      `dotnet build`, or `mypy --strict` for Python)
- [ ] No `any` / `Any` (except at external API boundaries)
- [ ] TypeScript: no double cast (`as unknown as`) in production **or** test code. Partial test
      data uses the repo's typed helpers (e.g. `satisfies PartialRootState as RootState`)
- [ ] All value objects used instead of raw primitives for domain fields

### Test Quality

- [ ] Backend: tests don't mock the database (real DB in fixtures)
- [ ] Frontend: interactions use the API from `house-rules.md` (e.g. `userEvent`, matching the
      installed major version), apart from its documented exceptions
- [ ] Each test has exactly one primary assertion
- [ ] Test factories / typed helpers used for complex objects
- [ ] Each acceptance scenario is tested at the lowest level that can prove it. Every new E2E test
      covers something a unit/component test cannot show; otherwise flag it as unneeded
- [ ] Coverage ≥ 85% branch

### Change Hygiene

- [ ] The diff follows the artifact policy: no `.specify/` files when the policy is "kept local"
- [ ] No absolute or machine-specific paths in committed files
- [ ] Commit titles use the module's commit scope from `house-rules.md`

### Spec Alignment

- [ ] Every `FR-NNN` from the spec is implemented
- [ ] Implementation matches the plan's file map
- [ ] No features added beyond the spec scope (YAGNI)

## Output Format

```markdown
## Code Review: $ARGUMENTS

### Overall: APPROVE | REQUEST_CHANGES | NEEDS_DISCUSSION

### Critical (must fix before merge)
- [file:line] issue and recommended fix

### Warnings (should fix)
- [file:line] issue and recommended fix

### Suggestions (optional)
- [file:line] improvement idea

### House rules
- Scan: <N rules checked, N hits, N exceptions> | `house-rules.md` missing
```

## Learning Loop

When a human or bot reviewer on the PR raises something this review missed, add it to
`house-rules.md` with the PR comment id as its source, a `Detect in diff` regex when the pattern can
be grepped, and the approved alternative. If the rule is not written in any repo doc, list it under
*Upstream Gaps*. This way the same correction does not have to be made on every PR.

$ARGUMENTS
