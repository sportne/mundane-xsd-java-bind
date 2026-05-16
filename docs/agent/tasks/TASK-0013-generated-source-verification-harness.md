# TASK-0013: generated-source-verification-harness

Status: draft.

Task ID: `TASK-0013`
Gate: Phase 3 generated model and writer vertical slice; starts only after `TASK-0012` is accepted.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-MODEL-001`, `REQ-XML-W-001`, `REQ-QA-001`, `REQ-BUILD-002`, `REQ-BUILD-003`, `REQ-NI-001`
ADR IDs: `ADR-0008`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/verification-plan.md`, `docs/verification/native-image-test-plan.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/native-image-architecture.md`, `docs/build/toolchain-matrix.md`
Target module: `modules/generator-core` and test-only support where needed
Allowed files: generator-core tests/resources/golden fixtures, generator-core test harness code, `modules/testing-support/src/test` or test-helper source if required by accepted design, and directly related verification/traceability docs
Forbidden files: CLI implementation, Gradle plugin implementation, runtime-jdkxml product adapters, public generator API source, dependency metadata, and behavior changes to generated readers/validation
Expected behavior: create the repeatable verification harness for generated model/writer source: golden comparison, generated-source compilation under the configured Java release, deterministic regeneration checks, static-analysis compatibility checks, first generated-code Native Image smoke coverage when the generated surface can execute with runtime primitives, and fixture organization for later reader/validation tasks.
Tests to add/update: harness self-tests, generated compile tests, deterministic double-run tests, golden mismatch failure tests where practical, generated-code native smoke tests when executable with runtime primitives, and Java 21/25 compatibility documentation linkage
Documentation to update: verification plan, coverage policy, traceability, and module README notes when the harness becomes canonical
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, generated-code native smoke command when the generated surface is executable or documented blocker, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: generated source verification is reusable by later reader, validation, CLI, and plugin tasks; generated-code native compatibility is mechanically checked as soon as there is an executable generated/runtime slice or the blocker is documented; no user-facing entry point is added; no runtime or generation scope broadening occurs
Rollback notes: revert harness source, tests, fixtures, and directly related docs

## Impact Notes

- Coverage: harness behavior must be tested because later task confidence depends on it.
- Native Image: add the first generated-code native smoke path once generated model/writer code can execute with runtime primitives; otherwise document the specific blocker.
- Security: generated-source tests must not require network access.
- Documentation: this task establishes the canonical golden fixture workflow.
