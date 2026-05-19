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
Expected behavior: expand the ongoing conformance and interop harness with selected W3C/external reference fixtures mapped to declared profiles, unsupported-feature diagnostics, and repeatable local comparison workflows.
Tests to add/update: conformance fixture classification tests, selected positive/negative profile tests, interop comparison tests, unsupported diagnostics, storage-policy checks where practical, documentation checks
Documentation to update: conformance strategy, W3C test-suite policy, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented conformance/interop commands, `git diff --check`
Acceptance criteria: selected tests are profile-mapped; unsupported tests prove deterministic diagnostics; external suite intake respects storage policy; interop evidence is repeatable without network access; no full-suite conformance claim is made
Rollback notes: revert harness, fixtures, scripts, and docs added by this task

## Accepted Planning Scope

- Add a small selected-fixture manifest or equivalent classification mechanism covering
  `supported-profile`, `unsupported-diagnostic`, `future-study`, and `blocked` cases.
- Use local fixtures or documented intake scripts only after license/storage review; do not vendor a
  broad W3C suite snapshot.
- Compare accepted XML behavior against JDK XML Schema validation and generated binding behavior.
  Compare generated output semantically, not as canonical bytes.
- Update conformance strategy, W3C suite policy, matrix, verification plan, and traceability with
  fixture IDs and limitations.

## Impact Notes

- Interop: this deepens the recurring interop lane established by prior slices.
- Native Image: selected conformance fixtures should be candidates for `TASK-0044`.
- Security: no untagged network access in tests.
- Documentation: full-suite claims remain forbidden unless proven.
