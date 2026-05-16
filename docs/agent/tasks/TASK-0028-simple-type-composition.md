# TASK-0028: simple-type-composition

Status: draft.

Task ID: `TASK-0028`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0027` is accepted.
Requirement IDs: future accepted `REQ-SCHEMA-*` and `REQ-VAL-*` for simple type list/union, `REQ-GEN-*`, `REQ-XML-W-001`, `REQ-XML-R-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: simple type parser/IR/binding/validation/emitter source and tests for accepted list/union behavior; golden fixtures; interop fixtures; directly related docs
Forbidden files: full datatype system beyond accepted scope, XSD 1.1 assertions, identity constraints, dependency metadata, runtime dependency additions
Expected behavior: implement accepted simple type composition behavior with generated lexical conversion, validation diagnostics, deterministic source, round-trip behavior where meaningful, and unsupported diagnostics for unaccepted list/union cases.
Tests to add/update: golden IR/binding/source tests, valid/invalid lexical tests, generated compile tests, reader/writer tests, interop comparisons, and negative diagnostics
Documentation to update: validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance checks, `git diff --check`
Acceptance criteria: accepted list/union fixtures work end to end; unaccepted cases fail explicitly; interop evidence is recorded where practical
Rollback notes: revert simple type composition implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare list/union validation outcomes with approved XML Schema validators where practical.
- Native Image: generated validators must remain explicit.
- Security: lexical handling must avoid unbounded input behavior.
- Documentation: support claims must not imply complete XSD datatype support.
