# TASK-0029: initial-derivation-support

Status: draft.

Task ID: `TASK-0029`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0028` is accepted.
Requirement IDs: planned `REQ-SCHEMA-010`, planned `REQ-BIND-002`, `REQ-MODEL-*`, planned `REQ-VAL-006`, `REQ-GEN-*`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: parser/IR/binding/emitter/validation source and tests for the accepted derivation subset; golden fixtures; interop fixtures; directly related docs
Forbidden files: unapproved full derivation semantics, substitution groups unless later accepted, wildcards, mixed content, dependency metadata
Expected behavior: implement `XP-XSD10-COMPOSED` initial derivation support for the accepted `TASK-0026` subset: named complex-type `xs:complexContent/xs:extension` flattened into records with base fields before derived fields and no generated Java inheritance, plus named simple restriction derivation chains over supported scalar bases with merged accepted facet metadata.
Tests to add/update: `T-DERIVATION-*` frontend, IR, binding, source, generated compile, reader/writer/validator, unsupported diagnostic, deterministic emission, round-trip, conformance, and interop comparisons
Documentation to update: generated-code contract if model shape changes, validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted derivation fixtures work end to end; model shape is documented; unsupported derivation is diagnosed; interop evidence is recorded
Rollback notes: revert derivation implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted derivation fixtures against approved XML Schema validation where practical.
- Native Image: generated inheritance or adapter shapes must remain statically reachable.
- Security: derivation graphs must detect cycles or excessive depth.
- Documentation: clarify which derivation forms remain unsupported.

## Accepted Implementation Shape

- Flatten complex extension at IR or binding time; generated Java model types remain records and do
  not extend generated base model classes.
- Preserve deterministic field order by emitting base attributes/elements before derived
  attributes/elements, with existing sequence and attribute rules applied after flattening.
- Merge simple restriction derivation chains only when every base and derived facet is in the
  accepted `XP-VALIDATION-10-BASIC` set and the final base scalar remains supported.
- Reject `simpleContent`, complex restriction, mixed content, abstract types, substitution groups,
  incompatible derivation graphs, cyclic derivation, and unsupported facet merging with deterministic
  diagnostics.
