# CleanYourTV Repository Constitution

**Version**: 1.0.0
**Status**: DRAFT - not ratified
**Ratified**:
**Relationship to repo rules**: Adds the SDD process on top of `AGENTS.md` and the team's docs. When they conflict, the repo-owned rule wins; raise the conflict with the team instead of overriding it here.

---

## Article I - Core Principles

### P1: Specification-First Development

Before implementation, each production feature or behavior change must have a `.specify/specs/<id>/spec.md` describing user stories, testable acceptance criteria, and success measures. This is the developer's adoption of the kit's proposed SDD policy (2026-10-26); existing repo workflows do not currently require a spec for every change.

### P2: Contracts Define Boundaries

Where a change creates or changes a module-card contract, config schema, or other shared interface, define the contract explicitly and version it with the spec. A breaking change requires a new spec and a migration or compatibility plan. (Developer decision, 2026-10-26; existing module-card contract: `docs/specs/card.md` and `docs/modules.md`.)

### P3: Tests Are the Spec Made Executable

Every acceptance scenario must map to an automated test where it can be exercised without a physical TV; device-dependent behavior must have a documented real-TV verification. No coverage percentage is set: CI has no coverage tool or threshold (`.github/workflows/ci.yml:12-70`; `house-rules.md`, Conflicts). The scenario-mapping requirement is adopted by the developer (2026-10-26).

### P4: Preserve the Repository's Actual Domain Contracts

This repository is an Android TV toolkit, not a web-service project. Keep TV-specific behavior in `config.xml` or a theme, use the existing JSON/XML contracts, and do not introduce Spring, JPA, REST DTO, or frontend conventions into the launcher. (`AGENTS.md:81-94`; `CONTRIBUTING.md:60-71`; `domain-model.md`.)

### P5: Shared Module Contracts Are Stable

Keep module-card and shared helper contracts compatible; do not weaken validation or silently change the behavior consumers rely on. Update the contract and its tests when a change is intentional. (Developer decision, 2026-10-26; `docs/specs/card.md`; `house-rules.md`, HR-24 to HR-26.)

---

## Article II - Development Workflow

### W1: Six-Step SDD Process

The developer adopted this process on 2026-10-26. Repo-specific safety, review, and contribution rules remain controlling.

| Step | Artifact | Tool |
| --- | --- | --- |
| 1. Constitution | `.specify/memory/constitution.md` | `/ai-sdlc/constitution` |
| 2. Specify | `.specify/specs/<id>/spec.md` | `/ai-sdlc/generate-spec` |
| 3. Plan | `.specify/specs/<id>/plan.md` | `/ai-sdlc/plan` |
| 4. Tasks | `.specify/specs/<id>/tasks.md` | `/ai-sdlc/tasks` |
| 5. Implement | Working code and tests | `/ai-sdlc/implement` |
| 6. Review | Review checklist | `/review/code` |

### W2: User-Provided Feature Input

Production feature work starts from the user's feature description and acceptance criteria; no external ticket or MCP integration is required. Derive a stable spec ID from the feature title, or use `SPEC-YYYY-NNN`. (Developer decision, 2026-10-26.) PR titles follow the repo's `area: what changed` convention and may include a GitHub issue reference when one exists (`AGENTS.md:159-168`).

### W3: Branch Naming

**Proposed - not adopted.** No branch naming convention is documented in the repo. Do not require `feature/` or `hotfix/` prefixes until the team adopts one. (Template proposal; no repo source.)

### W4: Artifact Policy

The developer's decision in this session (2026-10-26) is to commit `.specify/` artifacts. This conflicts with the repo's existing prohibition on committing agent/tool config or similar (`docs/DOS-AND-DONTS.md:33-34`; `house-rules.md`, HR-18) and its rule to leave unlinked files out (`AGENTS.md:114-128`). Repo-owned rules take precedence: this constitution records the requested policy but does not override those rules. The maintainer must reconcile the conflict in repo guidance before `.specify/` is committed.

---

## Article III - Domain Standards

### Android Launcher and TV Tooling

1. Keep the reference launcher plain Java on Android; it has `minSdk` 26, targets TVs with 1 GB of RAM, and stays offline. No network permission or features, telemetry, polling, background services, blur, video, or new launcher dependencies. (`AGENTS.md:129-138`; `CONTRIBUTING.md:60-68`; `house-rules.md`, HR-04, HR-09, HR-10.)
2. Keep new launcher behavior off by default; TV-specific values belong in `config.xml` or a theme, not device checks in code. (`AGENTS.md:88-90`; `CONTRIBUTING.md:68-69`; `docs/workflows/launcher-switch.md:1-9`; `house-rules.md`, HR-06, HR-30.)
3. Do not add permissions or grants. Use public Android APIs; do not use reflection to access hidden fields. Existing manifest permissions are not precedent for adding more. (`.github/pull_request_template.md:11-16`; `docs/DOS-AND-DONTS.md:60-61`; `house-rules.md`, HR-05, HR-24.)
4. Keep D-pad navigation deterministic. Verify launcher changes on a real TV, from every navigation edge, and attach a screenshot to the PR. (`CONTRIBUTING.md:68-73`; `AGENTS.md:122-125`; `.github/pull_request_template.md:11-16`.)
5. For a TV list, disable packages rather than uninstalling; never disable protected Android core packages or launch factory/test packages. Back up before changes and preserve the Home guard; stop if the device refuses the supported operation. (`AGENTS.md:17-48`; `house-rules.md`, HR-01, HR-02, HR-19 to HR-21.)
6. Choose the right home for work: tested lists and fixes here; looks in `themes/`; standalone cards in `addons/`; larger launcher changes in a fork and `SHOWCASE.md`. Keep one topic per PR. (`AGENTS.md:81-94, 159-168`; `CONTRIBUTING.md:7-18, 141-145`.)
7. Do not commit secrets or device identifiers, prebuilt binaries, or files that nothing links to. Report security problems privately. (`AGENTS.md:114-128, 131-157`; `SECURITY.md:1-24`; `house-rules.md`, HR-12, HR-13, HR-16.)

### Test Standards

1. Use JUnit 4 for Android-free launcher logic and Python `unittest` for tools, lists, and documentation. Follow existing test naming and fake-TV patterns. (`launcher/app/build.gradle`; `tests/test_home_guard.py`; `test-conventions.md`.)
2. Test behavior, not always-true conditions or exact prose wording. Keep script tests exercising the real script against the fake transport. (`docs/DOS-AND-DONTS.md`, Tools; `house-rules.md`, HR-25, HR-26.)
3. Every acceptance scenario maps to a test when automatable; run real-TV checks for behavior that depends on firmware or remote hardware. (Developer decision, 2026-10-26; `AGENTS.md:122-125`; `test-conventions.md`, E2E Policy.)
4. Changes to a launcher switch also require its config key, theme documentation, switch entry and real-TV screenshot, plus a JVM test for logic that can run without a TV. (`docs/workflows/launcher-switch.md:1-25`; `house-rules.md`, HR-06 to HR-08.)

---

## Article IV - Quality Gates

| Gate | Threshold | When Applied |
| --- | --- | --- |
| Python tests | `python3 -m unittest discover -s tests -v` passes; Python 3.12 and `tools/requirements.txt` | Every PR (`.github/workflows/ci.yml:10-25`; `AGENTS.md:116-118`) |
| Launcher tests, lint, build | JDK 17, Android SDK 34/build-tools 34.0.0; `testReleaseUnitTest lintRelease assembleRelease` succeeds, lint has zero errors | Every launcher PR (`.github/workflows/ci.yml:48-65`; `AGENTS.md:119`) |
| APK size | Less than 589,824 bytes | Every launcher build (`.github/workflows/ci.yml:66-70`; `AGENTS.md:120`) |
| Generated TV tables | `python3 tools/gen_tables.py --check` passes | Push to main; PR table update is intentionally deferred (`.github/workflows/ci.yml:27-45`) |
| Real-device verification | Run on a real TV; D-pad all edges and screenshot | Launcher PRs (`AGENTS.md:122-125`; `.github/pull_request_template.md:11-16`) |
| Coverage | No percentage threshold configured | Do not claim 85%; `.github/copilot-instructions.md:40` conflicts with measured CI (`test-conventions.md`, Framework) |
| Spec readiness | Resolve `[NEEDS CLARIFICATION]` before planning | Every SDD change (developer adoption, 2026-10-26) |
| Security review | **Proposed - not adopted** as a mandatory gate; security reports must be private | Security issues follow `SECURITY.md:1-24` |

---

## Article V - Amendment Process

Amendments require:

1. A spec describing the proposed change and rationale. (Developer adoption, 2026-10-26.)
2. Review by at least two team members. (Developer adoption, 2026-10-26; an additional governance rule beyond the current PR guidance.)
3. Update this document with a version bump. Set a ratification date only after the team explicitly ratifies the amendment. (Developer adoption, 2026-10-26; no ratification date was provided for this draft.)

---

## Repo Rules Requiring Reconciliation

- **Artifact policy:** the requested committed `.specify/` policy conflicts with `docs/DOS-AND-DONTS.md` and `AGENTS.md`; do not use this draft to bypass them.
- **Stack and coverage:** `.github/copilot-instructions.md` contains Spring-specific rules and an 85% coverage target that do not match the Android/Python code or CI. Keep those out of this constitution; actual CI gates above control.
- **Feature input:** specs use the user's description directly; external ticket systems are optional context only and are not part of the required workflow.
- **Existing safety rules:** this constitution summarizes the high-impact rules; `PROMPT.md`, `AGENTS.md`, the workflow guides, and `docs/DOS-AND-DONTS.md` remain authoritative in full.
