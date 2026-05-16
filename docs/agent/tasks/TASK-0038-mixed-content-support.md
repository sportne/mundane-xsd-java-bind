# TASK-0038: mixed-content-support

Status: draft.

Task ID: `TASK-0038`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0037` is accepted.
Requirement IDs: future accepted mixed-content schema, binding, model, XML reader/writer, validation, generation, and QA IDs
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/conformance/matrix.md`
Target modules: generator/runtime modules approved by `TASK-0036`, conformance tests, examples
Allowed files: mixed-content parser/IR/binding/runtime/emitter/reader/writer/validation source and tests, golden fixtures, interop fixtures, directly related docs
Forbidden files: unapproved DOM-first runtime, XSD 1.1 assertions, dependency metadata unless approved, canonicalization changes reserved for `TASK-0039`
Expected behavior: implement accepted mixed-content support with deterministic generated model shape, read/write ordering semantics, validation behavior, and unsupported diagnostics for out-of-scope mixed-content constructs.
Tests to add/update: golden source tests, generated compile tests, read/write ordering tests, round-trip tests, invalid mixed-content diagnostics, interop comparisons
Documentation to update: generated-code contract, runtime architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted mixed-content fixtures work end to end; ordering semantics are documented and tested; interop evidence is recorded
Rollback notes: revert mixed-content implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare mixed-content fixture validity and serialization behavior where practical.
- Native Image: mixed-content representation remains reflection-free.
- Security: text size and nesting limits remain testable.
- Documentation: describe generated mixed-content model shape explicitly.
