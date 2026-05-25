# TASK-0090: ir-particle-wildcard-normalization

Status: draft.

Task ID: `TASK-0090`
Priority: P2
Gate: deeper generator architecture refactor.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-016`, `REQ-SCHEMA-017`, `REQ-QA-002`
ADR IDs: `ADR-0001`, `ADR-0006`, `ADR-0007`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/compiler-pipeline.md`, `docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: schema IR package-private normalization helpers and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public API changes, profile behavior changes, schema support expansion, generated
source behavior changes, release metadata, dependency metadata, and quality-gate weakening.
Expected behavior: extract content-particle, attribute, and wildcard normalization policy from
`SchemaIrBuilder` into focused package-private helpers while preserving IR output and diagnostics.
Tests to add/update: focused helper tests plus existing schema IR and generated-code behavior locks.
Documentation to update: complexity review, compiler pipeline if vocabulary changes, traceability,
and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: particle/attribute/wildcard normalization has explicit helper ownership;
`SchemaIrBuilder.build(...)` behavior remains unchanged.
Rollback notes: revert schema IR helper extraction, tests, and docs.
