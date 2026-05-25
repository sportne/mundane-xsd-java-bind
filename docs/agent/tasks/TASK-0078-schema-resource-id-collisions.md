# TASK-0078: schema-resource-id-collisions

Status: draft.

Task ID: `TASK-0078`
Priority: P0
Gate: post-1.0.0 follow-up correctness.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-004`, `REQ-QA-002`
ADR IDs: `ADR-0001`, `ADR-0006`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/naming-collision-review.md`,
`docs/architecture/compiler-pipeline.md`, `docs/verification/xsd10-full-feature-matrix.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/**`,
`modules/generator-core/src/test/java/**`, directly related verification/conformance docs,
traceability docs, and this task/handoff.
Forbidden files: release metadata, dependency metadata, publishing/signing configuration, W3C suite
vendoring, schema support expansion unrelated to resource identity, and quality-gate weakening.
Expected behavior: schema identity remains stable and deterministic when two schema files share the
same basename in different directories. Resource IDs must preserve enough relative path context to
avoid resolver, parser, IR, and diagnostic collisions while remaining readable in diagnostics.
Tests to add/update: resolver/parser/IR tests for duplicate basenames under different directories,
CoreGenerator generated-source regression proving same-name files in different directories can
generate when schema symbols are otherwise valid, and diagnostics for true duplicate components.
Documentation to update: naming-collision review, compiler/verification docs if resource ID policy
changes, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: duplicate schema basenames no longer collide solely because their filenames
match; true duplicate schema components still report deterministic diagnostics; generated output is
unchanged except for corrected diagnostics/resource identity behavior.
Rollback notes: revert resolver/resource-ID changes and related regression tests/docs.

