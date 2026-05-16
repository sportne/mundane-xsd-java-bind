# TASK-0013: generated-source-verification-harness

Status: draft.

Task ID: `TASK-0013`
Gate: Phase 3 generated model and writer vertical slice; starts only after `TASK-0012` is accepted.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-MODEL-001`, `REQ-XML-W-001`, `REQ-QA-001`, `REQ-BUILD-002`, `REQ-BUILD-003`
ADR IDs: `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/verification-plan.md`, `docs/architecture/generated-code-contract.md`, `docs/build/toolchain-matrix.md`
Target module: `modules/generator-core` and test-only support where needed
Allowed files: generator-core tests/resources/golden fixtures, generator-core test harness code, `modules/testing-support/src/test` or test-helper source if required by accepted design, and directly related verification/traceability docs
Forbidden files: CLI implementation, Gradle plugin implementation, runtime-jdkxml product adapters, public generator API source, dependency metadata, and behavior changes to generated readers/validation
Expected behavior: create the repeatable verification harness for generated model/writer source: golden comparison, generated-source compilation under the configured Java release, deterministic regeneration checks, static-analysis compatibility checks, and fixture organization for later reader/validation tasks.
Tests to add/update: harness self-tests, generated compile tests, deterministic double-run tests, golden mismatch failure tests where practical, and Java 21/25 compatibility documentation linkage
Documentation to update: verification plan, coverage policy, traceability, and module README notes when the harness becomes canonical
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: generated source verification is reusable by later reader, validation, CLI, and plugin tasks; no user-facing entry point is added; no runtime or generation scope broadening occurs
Rollback notes: revert harness source, tests, fixtures, and directly related docs

## Impact Notes

- Coverage: harness behavior must be tested because later task confidence depends on it.
- Native Image: no native image execution yet.
- Security: generated-source tests must not require network access.
- Documentation: this task establishes the canonical golden fixture workflow.
