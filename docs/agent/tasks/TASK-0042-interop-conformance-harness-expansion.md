# TASK-0042: interop-conformance-harness-expansion

Status: draft.

Task ID: `TASK-0042`
Gate: `0.6.0` Hardening and Release Maturity; starts only after `TASK-0041` is accepted.
Requirement IDs: accepted conformance and interop IDs, future `REQ-QA-*`, `REQ-DOC-*`
ADR IDs: `ADR-0001`, `ADR-0006`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/verification/conformance-strategy.md`, `docs/conformance/w3c-test-suite-policy.md`, `docs/conformance/matrix.md`
Target modules: `modules/conformance-tests`, `modules/testing-support`, examples as needed
Allowed files: conformance harness source/tests/fixtures, documented test-suite intake scripts if approved, interop fixtures, docs
Forbidden files: broad vendoring of external suites without license/maintenance review, unsupported feature implementation, dependency metadata unless approved, quality-gate weakening
Expected behavior: expand the ongoing conformance and interop harness with selected W3C/external fixtures mapped to declared profiles, unsupported-feature diagnostics, and repeatable comparison workflows.
Tests to add/update: conformance fixture classification tests where practical, selected positive/negative profile tests, interop comparison tests, unsupported diagnostics, documentation checks
Documentation to update: conformance strategy, W3C test-suite policy, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented conformance/interop commands, `git diff --check`
Acceptance criteria: selected tests are profile-mapped; external suite intake respects storage policy; interop evidence is repeatable
Rollback notes: revert harness, fixtures, scripts, and docs added by this task

## Impact Notes

- Interop: this deepens the recurring interop lane established by prior slices.
- Native Image: selected conformance fixtures should be candidates for `TASK-0044`.
- Security: no untagged network access in tests.
- Documentation: full-suite claims remain forbidden unless proven.
