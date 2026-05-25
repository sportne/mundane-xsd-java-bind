# TASK-0084: ir-normalization-refactor-tranche

Status: accepted.

Task ID: `TASK-0084`
Priority: P2
Gate: generator architecture refactor.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-014`, `REQ-QA-002`
ADR IDs: `ADR-0006`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/compiler-pipeline.md`, `docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: generator-core schema IR builder/helper classes and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public generator API changes, schema support expansion, generated output changes,
dependency metadata, release metadata, and quality-gate weakening.
Expected behavior: start the IR normalization refactor by extracting low-risk package-private
normalization policy helpers from `SchemaIrBuilder` while preserving all public
`SchemaIrBuilder.build(...)` behavior, diagnostics, ordering, and generated output.
Tests to add/update: characterization tests around occurrence/cardinality policy, QName/type
reference normalization, and deterministic diagnostics using existing `SchemaIrBuilderTest` or
adjacent focused tests.
Documentation to update: complexity review, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: package-private helper boundaries exist for the selected normalization policy;
schema indexing, profile gates, component graph ownership, diagnostics, and output behavior remain
unchanged; no support claims or public APIs change.
Rollback notes: revert helper extraction, characterization tests, and docs.

Completion notes:
- Extracted package-private `SchemaIrNormalizationPolicy` for low-risk IR normalization policy:
  occurrence/cardinality parsing, QName lexical resolution, cardinality composition, and
  deterministic diagnostic creation/sorting.
- Kept schema indexing, profile gates, component graph ownership, `SchemaIrBuilder.build(...)`
  behavior, diagnostics, and generated output unchanged.
- Added focused helper characterization tests for default/explicit cardinality, invalid occurrence
  diagnostics, QName resolution diagnostics, cardinality composition, and diagnostic ordering.
- Updated architecture, traceability, and handoff docs without public API or support-claim changes.

Evidence:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
