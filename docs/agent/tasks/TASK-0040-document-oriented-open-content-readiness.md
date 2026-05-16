# TASK-0040: document-oriented-open-content-readiness

Status: draft.

Task ID: `TASK-0040`
Gate: `0.5.0` Document-Oriented and Open Content readiness; starts only after `TASK-0039` is accepted.
Requirement IDs: accepted `0.5.0` schema, binding, model, XML, validation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.5.0` document-oriented/open-content vertical slice with conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.5.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native command, `git diff --check`
Acceptance criteria: open-content support claims match evidence; formal canonicalization claims are avoided unless proven; interop evidence is recorded
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness requires recorded validation and serialization evidence or explicit limitation notes.
- Native Image: representative open-content fixtures should run in selected lanes.
- Security: unknown/mixed content limits remain verified.
- Documentation: distinguish supported document-oriented cases from full XML ecosystem support.
