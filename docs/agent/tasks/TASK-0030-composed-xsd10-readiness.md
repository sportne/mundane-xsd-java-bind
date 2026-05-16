# TASK-0030: composed-xsd10-readiness

Status: draft.

Task ID: `TASK-0030`
Gate: `0.3.0` Composed XSD 1.0 Schemas readiness; starts only after `TASK-0029` is accepted.
Requirement IDs: accepted `0.3.0` schema, binding, generation, validation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.3.0` Composed XSD 1.0 vertical slice with honest conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.3.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native smoke or conformance command, `git diff --check`
Acceptance criteria: support claims match tested behavior; interop evidence is recorded; unsupported XSD 1.0 features remain explicit; next slice remains draft
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness cannot pass without recorded interop evidence or an explained limitation.
- Native Image: representative composed-schema fixtures should run in selected lanes.
- Security: composition depth/cycle protections must be verified.
- Documentation: no full-XSD conformance claim unless matrix supports it.
