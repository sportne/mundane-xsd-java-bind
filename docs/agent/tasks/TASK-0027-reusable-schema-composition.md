# TASK-0027: reusable-schema-composition

Status: draft.

Task ID: `TASK-0027`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0026` is accepted.
Requirement IDs: future accepted `REQ-SCHEMA-*` for named model groups and attribute groups, `REQ-GEN-*`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-*`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: parser/IR/binding/emitter/validation source and tests for accepted named model group and attribute group behavior; golden fixtures; conformance fixtures; directly related docs
Forbidden files: unapproved derivation, wildcards, substitution groups, mixed content, dependency metadata, runtime dependency additions
Expected behavior: implement accepted reusable schema composition features through the pipeline with deterministic generated model/reader/writer/validation behavior and explicit unsupported diagnostics for out-of-scope group constructs.
Tests to add/update: golden IR/binding/source tests, generated compile tests, group reuse tests, invalid reference diagnostics, round-trip tests, interop fixtures, and Native Image fixtures if selected
Documentation to update: conformance matrix, compiler pipeline docs, traceability matrix, compatibility profiles
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted group constructs work end to end; invalid or unsupported group constructs fail deterministically; interop evidence is recorded
Rollback notes: revert composition implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted group fixtures with external validation where practical.
- Native Image: generated paths must remain reflection-free.
- Security: recursive composition must be bounded and diagnostic-safe.
- Documentation: support claims must name the accepted group subset.
