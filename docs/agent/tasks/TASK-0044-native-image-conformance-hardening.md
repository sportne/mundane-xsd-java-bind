# TASK-0044: native-image-conformance-hardening

Status: draft.

Task ID: `TASK-0044`
Gate: `0.6.0` Hardening and Release Maturity; starts only after `TASK-0043` is accepted.
Requirement IDs: `REQ-NI-001`, accepted conformance, interop, generated-code, runtime, QA, and build IDs
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/native-image-test-plan.md`, `docs/architecture/native-image-architecture.md`, `docs/infrastructure/ci-plan.md`
Target areas: Native Image workflows, examples, conformance tests, generated binding fixtures, docs
Allowed files: native-image CI/workflow/config, native conformance tests, selected fixtures, docs
Forbidden files: reflection configuration to hide architecture issues unless approved by ADR, quality-gate weakening, unsupported feature implementation
Expected behavior: harden Native Image from smoke testing into selected conformance execution for representative generated bindings across supported profiles and interop-derived fixtures.
Tests to add/update: native conformance tests, generated binding native round trips, diagnostics tests under native execution, CI validation
Documentation to update: native-image plan, CI plan, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented native conformance command, `git diff --check`
Acceptance criteria: selected conformance fixtures run under Native Image; reflection/proxy/resource failures are treated according to ADR-0010; evidence is documented
Rollback notes: revert native workflow/tests/fixtures/docs from this task

## Impact Notes

- Interop: include selected interop fixtures in native lanes where practical.
- Native Image: this task is the Native Image conformance hardening step.
- Security: native paths must preserve resolver and reader denial behavior.
- Documentation: record unsupported native environments honestly.
