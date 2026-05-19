# TASK-0041: hardening-release-maturity-planning

Status: accepted.

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

## Accepted `0.6.0` Planning Scope

`TASK-0041` defines `0.6.0` as hardening and release maturity, not new schema feature expansion.
The accepted implementation sequence remains:

- `TASK-0042`: selected conformance and interop harness expansion.
- `TASK-0043`: performance, memory, and streaming benchmark baselines.
- `TASK-0044`: selected Native Image conformance hardening beyond smoke tests.
- `TASK-0045`: release engineering and publication dry-run readiness.
- `TASK-0046`: final hardening/release maturity readiness review.

The slice must preserve the accepted `TASK-0040` `XP-XSD10-DOCUMENT` baseline. Later tasks may add
fixtures, benchmarks, Native Image conformance execution, CI lanes, and release dry-run validation,
but they must not add unsupported product behavior, weaken quality gates, claim full XSD 1.0 or XSD
1.1 conformance, claim XML Canonicalization, publish artifacts, or create release tags unless a
separate approved task explicitly authorizes that action.

## Acceptance Evidence

- Documented selected W3C/external fixture intake categories, repeatable interop comparison
  expectations, and storage-policy limits.
- Documented planned benchmark, streaming/memory, Native Image conformance, CI, coverage, release
  dry-run, and final readiness posture for `0.6.0`.
- Added designed traceability placeholders for conformance/interop hardening, performance
  baselines, selected Native Image conformance, release readiness, and hardening QA.
- Refined `TASK-0042` through `TASK-0046` with decision-complete planning scope, evidence
  expectations, and release/non-claim boundaries.
- Verification completed:
  - `./gradlew validateDesignControlPack qualityGate --console=plain`
  - `git diff --check`

## Impact Notes

- Interop: define ongoing interop depth and fixtures, not a one-time final check.
- Native Image: select conformance fixtures for native execution.
- Security: include security regression gates in release readiness.
- Documentation: release gates must not overstate conformance.
