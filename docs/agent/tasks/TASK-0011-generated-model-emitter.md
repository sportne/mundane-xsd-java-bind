# TASK-0011: generated-model-emitter

Status: draft.

Task ID: `TASK-0011`
Gate: Phase 3 generated model and writer vertical slice; starts only after `TASK-0010` is accepted.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-MODEL-001`, `REQ-NS-001`, `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-RT-001`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0006`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/compiler-pipeline.md`, `docs/architecture/module-boundaries.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/io/github/mundanej/mxjb/generator/core/emit/**`, generator-core tests/resources/golden generated model fixtures, and directly related docs
Forbidden files: runtime module source except tests depending on accepted runtime-core interfaces, public `generator-api` source, CLI source, Gradle plugin source, XML reader/writer emitter implementation, validation engine implementation, dependency metadata, and committed generated product code outside approved golden fixtures
Expected behavior: emit deterministic Java 21 model source for supported binding-model constructs using immutable records or final classes/builders where appropriate, defensive copies for repeated fields, null-free collection invariants, stable package/type/member names, and no runtime annotations or reflection-based binding behavior.
Tests to add/update: golden source tests, generated model compile tests, repeated/optional field behavior tests, name collision tests, static-analysis compatibility tests, and ArchUnit checks for forbidden generated-code dependencies and reflection behavior
Documentation to update: generated-code contract and traceability docs when generated model shape is finalized
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: generated model source is byte-for-byte deterministic, compiles under Java 21, follows the generated-code contract, and does not introduce reader/writer/validation behavior
Rollback notes: revert model emitter source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: generator-core emitter tests must cover both source text and compiled behavior.
- Native Image: generated models must not require reflection metadata.
- Security: no XML or external resource access is added.
- Documentation: golden outputs are allowed only as approved test artifacts.
