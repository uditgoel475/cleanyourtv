---
name: review-security
description: "Security review covering OWASP Top 10, PII handling, and data access controls"
argument-hint: "[<SPEC-ID> or file paths] [-svc <service> | -app <app>]"
---

# /review/security

Perform a security review of the current or specified changes.

## Scope

Focus on files in `$ARGUMENTS` if provided, otherwise all changed files.

## OWASP Top 10 Checks

### A01 — Broken Access Control

- [ ] All endpoints (except `/health`) require authentication via `require_auth`
- [ ] User-scoped resources verified against requesting user identity
- [ ] No direct object references without ownership validation
- [ ] Role checks not bypassed via parameter manipulation

### A02 — Cryptographic Failures

- [ ] No secrets, tokens, or passwords in source code or logs
- [ ] PII not stored in plain text in DB or logs
- [ ] Connection strings use environment variables, not literals

### A03 — Injection

- [ ] All SQL uses parameterised queries (bound parameters / prepared statements) — never string concatenation
- [ ] No `f"SELECT ... {user_input}"` patterns
- [ ] Formula/expression parsers use AST validation, not `eval()`
- [ ] File paths constructed from user input are validated

### A04 — Insecure Design

- [ ] Draft/publish state machine cannot be bypassed
- [ ] Bulk operations (CSV upload) validated before DB write
- [ ] Rate limiting or size limits on file upload endpoints

### A05 — Security Misconfiguration

- [ ] CORS configured to allowlist only required origins
- [ ] Debug endpoints not exposed in production builds
- [ ] Error responses do not leak stack traces or internal paths

### A07 — Authentication Failures

- [ ] Auth header (e.g. `Authorization: Bearer …` or `X-Auth-Token`) validated by the configured auth provider on every non-health request
- [ ] Expired or invalid session tokens rejected (401)
- [ ] User identity not derived from request body — always from auth system

### A09 — Logging Failures

- [ ] Request ID logged with every error
- [ ] PII fields (email, names) not logged at DEBUG level
- [ ] Log levels appropriate: errors at ERROR, normal ops at INFO

## PII / Data Classification

- [ ] PII columns identified in the spec's data model
- [ ] PII columns masked or pseudonymised before writing to non-prod environments

## Data Access Controls

- [ ] Database grants follow least-privilege
- [ ] Service accounts, not personal tokens, used in job/service definitions
- [ ] SAS URLs for blob storage have minimal permissions and short expiry (≤ 1h)

## Output Format

```markdown
## Security Review: $ARGUMENTS

### Status: CLEAN | FINDINGS | CRITICAL_BLOCK

### Critical (block merge)
- [file:line] vulnerability, CWE reference, fix

### High (fix before release)
- [file:line] vulnerability, recommended fix

### Medium / Low
- [file:line] risk, recommended fix
```

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
