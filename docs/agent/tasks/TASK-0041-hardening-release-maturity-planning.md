# TASK-0041: hardening-release-maturity-planning

Status: draft.

Task ID: `TASK-0041`
Gate: `0.6.0` Hardening and Release Maturity planning; starts only after `TASK-0040` is accepted.
Requirement IDs: future `REQ-PERF-*`, future `REQ-REL-*`, accepted conformance, interop, Native Image, QA, build, and docs IDs
ADR IDs: `ADR-0010`, `ADR-0011`, `ADR-0012`, `ADR-0013`, plus any ADRs changed by release policy
Specification references: `docs/verification/conformance-strategy.md`, `docs/verification/native-image-test-plan.md`, `docs/infrastructure/release-plan.md`, `docs/infrastructure/ci-plan.md`
Target areas: verification, conformance, performance, CI, release engineering, and task cards
Allowed files: requirements docs, verification docs, conformance docs, infrastructure docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate weakening
Expected behavior: define selected W3C conformance suite intake, interop depth, performance targets, streaming goals, Native Image conformance scope, publication workflow, and release gates for `0.6.0`.
Tests to add/update: planned tests only for `TASK-0042` through `TASK-0046`
Documentation to update: conformance strategy, native-image plan, release plan, CI plan, coverage policy, traceability, follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: hardening scope is decision-complete; performance and release claims have measurable acceptance criteria; interop remains a continuing lane
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: define ongoing interop depth and fixtures, not a one-time final check.
- Native Image: select conformance fixtures for native execution.
- Security: include security regression gates in release readiness.
- Documentation: release gates must not overstate conformance.
