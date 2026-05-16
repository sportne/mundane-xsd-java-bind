# TASK-0025: practical-data-contracts-readiness

Status: draft.

Task ID: `TASK-0025`
Gate: `0.2.0` Practical Data Contracts readiness; starts only after `TASK-0024` is accepted.
Requirement IDs: `REQ-SCHEMA-007`, `REQ-VAL-003`, `REQ-GEN-*`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-NI-001`, `REQ-QA-001`, `REQ-DOC-*`
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.2.0` Practical Data Contracts vertical slice, including `XP-DATA-10-CHOICE`, accepted `XP-VALIDATION-10-BASIC` facets, conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.2.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native smoke command, `git diff --check`
Acceptance criteria: docs do not overclaim support; requirement and conformance statuses match test evidence; interop evidence is recorded; next slice remains draft
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness cannot pass without explicit interop evidence or an explained gap.
- Native Image: include choice/facet fixtures if selected in planning.
- Security: ensure diagnostics remain stable and safe.
- Documentation: record known unsupported cases clearly.
