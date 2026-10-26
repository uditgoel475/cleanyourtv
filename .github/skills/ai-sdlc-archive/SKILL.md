---
name: ai-sdlc-archive
description: "Archive a completed change: verify readiness, merge delta capability specs into the target's living specs, promote accepted ADRs, and move the change to openspec/changes/archive/. Counterpart to /ai-sdlc/change."
argument-hint: "<change-name> [in <target>]"
---

# /ai-sdlc/archive

Archive change: **$ARGUMENTS**

Finalizes a change created by `/ai-sdlc/change`. It folds the change's **delta**
specs into the target's **living** capability specs so the repository always answers
"what does this capability do now?" — the archive step from
[ADR-006](../../../docs/decisions/ADR-006-openspec-vs-speckit-spec-generation.md) and
[docs/SPEC_GENERATION_MODEL.md](../../../docs/SPEC_GENERATION_MODEL.md).

Only `service-change` / `capability-change` work is archived. `ticket-feature` work
has no living-spec merge and is not archived here.

## Argument parsing

`$ARGUMENTS` is `<change-name> [in <target>]`:

- `<change-name>` — the change folder name (e.g. `nats-role-event-publish`).
- `in <target>` — optional service/app/package or path. If omitted, resolve the
  change by searching `*/openspec/changes/<change-name>/`. If more than one match,
  list them and ask which target.

Set `$CHANGE_DIR = <target>/openspec/changes/<change-name>/` and
`$SPECS_ROOT = <target>/openspec/specs/` and `$ADRS_ROOT = <target>/openspec/adrs/`.

## Workflow

### Step 1 — Locate and load the change

1. Resolve `$CHANGE_DIR`. If it does not exist, list active changes under
   `*/openspec/changes/` (excluding `archive/`) and stop.
2. Read `proposal.md`, `tasks.md`, every `specs/*/spec.md`, and any `adrs/*.md`.

### Step 2 — Archive-readiness gate (BLOCKING)

Do not archive if any of the following holds. STOP with a clear reason:

- any task checkbox in `tasks.md` is unchecked, **including** the Final Review section;
- the Final Review drift sweep is not resolved to zero open items;
- any ADR in `$CHANGE_DIR/adrs/` still has `Status: Proposed` (must be `Accepted`,
  `Rejected`, or `Deferred` with rationale);
- a delta spec violates the format (missing scenario, `MODIFIED` block not complete,
  `REMOVED` missing Reason/Migration, scenarios not using exactly four `####`);
- `/ai-sdlc/verify` has not been run for this change in-session (run it now if unsure).

### Step 3 — Promote accepted ADRs

For each `Accepted` ADR working copy in `$CHANGE_DIR/adrs/NNNN-*.md`:

1. Copy it to `$ADRS_ROOT/NNNN-*.md` (the permanent record) if not already present.
2. Confirm the numbering does not collide with existing `$ADRS_ROOT` ADRs.
3. Leave `Rejected` / `Deferred` ADRs in the change folder only (do not promote).

The change cannot be archived if an `Accepted` ADR is missing from `$ADRS_ROOT`.

### Step 4 — Merge deltas into living specs

For each `$CHANGE_DIR/specs/<capability>/spec.md`, apply its delta to
`$SPECS_ROOT/<capability>/spec.md` (create the living spec if the capability is new):

- **ADDED Requirements** → append each requirement (with its scenarios) to the living spec.
- **MODIFIED Requirements** → replace the matching `### Requirement: <name>` block in
  the living spec with the change's full updated block. Match header text
  whitespace-insensitively; if no match is found, STOP and report (do not duplicate).
- **REMOVED Requirements** → delete the matching requirement from the living spec.
  Keep a one-line note referencing the change name and migration.
- **RENAMED Requirements** → apply the FROM:/TO: rename in place.

After merging, the living specs must contain no delta headers (`## ADDED/MODIFIED/…`)
— they describe current state, not the change.

### Step 5 — Move the change to archive

1. Move `$CHANGE_DIR` to `<target>/openspec/changes/archive/<change-name>/`.
2. Preserve the archived `proposal.md`, `design.md`, `tasks.md`, delta specs, and
   ADR working copies as the historical record.

### Step 6 — Update the ledger and report

1. If the target keeps a backlog/index doc, mark the change complete there.
2. Emit a summary: capabilities merged (ADDED/MODIFIED/REMOVED counts), ADRs promoted,
   the archive path, and any handoff targets still pending their own `/ai-sdlc/change`.

**Verify after archiving:** show that `$SPECS_ROOT` contains the merged requirements
and `$CHANGE_DIR` no longer exists outside `archive/` — never claim success without
this evidence (see `/ai-sdlc/verify`).

$ARGUMENTS

<!-- ai-sdlc:generated — edit the skill in .claude/commands/, or override via project/commands/, not this file -->
