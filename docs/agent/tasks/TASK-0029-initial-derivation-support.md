# TASK-0029: initial-derivation-support

Status: draft.

Task ID: `TASK-0029`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0028` is accepted.
Requirement IDs: future accepted `REQ-SCHEMA-*`, `REQ-BIND-*`, `REQ-MODEL-*`, `REQ-VAL-*`, `REQ-GEN-*`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: parser/IR/binding/emitter/validation source and tests for the accepted derivation subset; golden fixtures; interop fixtures; directly related docs
Forbidden files: unapproved full derivation semantics, substitution groups unless later accepted, wildcards, mixed content, dependency metadata
Expected behavior: implement the accepted subset of simple and/or complex derivation with deterministic generated model shape, reader/writer/validation behavior, and explicit diagnostics for unsupported derivation cases.
Tests to add/update: derivation golden IR/binding/source tests, generated compile tests, valid/invalid XML tests, round-trip tests, unsupported diagnostics, and interop comparisons
Documentation to update: generated-code contract if model shape changes, validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted derivation fixtures work end to end; model shape is documented; unsupported derivation is diagnosed; interop evidence is recorded
Rollback notes: revert derivation implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted derivation fixtures against approved XML Schema validation where practical.
- Native Image: generated inheritance or adapter shapes must remain statically reachable.
- Security: derivation graphs must detect cycles or excessive depth.
- Documentation: clarify which derivation forms remain unsupported.
