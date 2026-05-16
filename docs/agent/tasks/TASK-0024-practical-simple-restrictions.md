# TASK-0024: practical-simple-restrictions

Status: draft.

Task ID: `TASK-0024`
Gate: `0.2.0` Practical Data Contracts; starts only after `TASK-0023` is accepted.
Requirement IDs: `REQ-VAL-003`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-R-001`, `REQ-XML-W-001`, `REQ-MODEL-001`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0022`
Allowed files: simple-type parsing/IR/binding/validation/emitter source and tests for accepted enum, length, range, and pattern behavior; golden fixtures; conformance fixtures; directly related docs
Forbidden files: full datatype system, list/union unless approved in a later slice, identity constraints, dependency metadata, runtime dependency additions, XSD 1.1 assertions
Expected behavior: expand practical simple restrictions for generated validation and diagnostics, covering the accepted enum, length, range, and pattern subset with deterministic generated source and runtime behavior.
Tests to add/update: golden IR/binding/source tests, generated compile tests, valid/invalid lexical tests, diagnostics tests with locations where available, round-trip tests preserving accepted lexical semantics, and interop comparisons where practical
Documentation to update: validation architecture, conformance matrix, compatibility profiles, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks named by the implementation, `git diff --check`
Acceptance criteria: accepted facets are generated and tested; unsupported facets produce explicit diagnostics; JDK/XML interop evidence is recorded where practical
Rollback notes: revert simple restriction implementation, tests, fixtures, golden outputs, and directly related docs

## Impact Notes

- Interop: compare representative valid/invalid facet fixtures with approved XML Schema validation where practical.
- Native Image: generated validators must remain explicit and reflection-free.
- Security: pattern handling must avoid unbounded or unsafe behavior.
- Documentation: keep support claims facet-specific.
