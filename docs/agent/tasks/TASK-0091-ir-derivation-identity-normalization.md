# TASK-0091: ir-derivation-identity-normalization

Status: draft.

Task ID: `TASK-0091`
Priority: P2
Gate: deeper generator architecture refactor.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-018`, `REQ-SCHEMA-019`, `REQ-QA-002`
ADR IDs: `ADR-0001`, `ADR-0006`, `ADR-0007`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/compiler-pipeline.md`, `docs/architecture/validation-architecture.md`,
`docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: schema IR package-private derivation/identity helpers and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public API changes, profile behavior changes, schema support expansion, identity
semantics changes, release metadata, dependency metadata, and quality-gate weakening.
Expected behavior: extract derivation normalization/checking and identity selector/field path
normalization from `SchemaIrBuilder` into focused package-private helpers while preserving IR output
and diagnostics.
Tests to add/update: focused helper tests plus existing schema IR, derivation, identity, and
generated-validation behavior locks.
Documentation to update: complexity review, compiler/validation docs if vocabulary changes,
traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: derivation and identity normalization have explicit helper ownership;
accepted derivation and identity behavior is unchanged.
Rollback notes: revert schema IR helper extraction, tests, and docs.
