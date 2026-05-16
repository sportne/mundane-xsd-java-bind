# TASK-0032: nillable-default-fixed-semantics

Status: draft.

Task ID: `TASK-0032`
Gate: `0.4.0` XSD 1.0 Semantic Expansion; starts only after `TASK-0031` is accepted.
Requirement IDs: future accepted `REQ-SCHEMA-*`, `REQ-BIND-*`, `REQ-MODEL-*`, `REQ-VAL-*`, `REQ-GEN-*`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, runtime modules if accepted by planning, conformance tests, examples
Allowed files: parser/IR/binding/emitter/validation source and tests for accepted `nillable`, `default`, and `fixed` behavior; golden fixtures; interop fixtures; directly related docs
Forbidden files: substitution groups, mixed content, wildcards, XSD 1.1 assertions, dependency metadata unless approved by ADR
Expected behavior: implement accepted `nillable`, `default`, and `fixed` semantics across generated model shape, reader/writer behavior, validation diagnostics, deterministic source, and round-trip/conformance fixtures.
Tests to add/update: golden source tests, generated compile tests, valid/invalid XML tests, default/fixed/nil diagnostics, round-trip tests, and interop comparisons
Documentation to update: generated-code contract, validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted semantic fixtures work end to end; model shape is stable and documented; interop evidence is recorded
Rollback notes: revert semantic implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare default/fixed/nillable behavior against approved XML Schema validation where practical.
- Native Image: generated semantic paths remain reflection-free.
- Security: diagnostics must not leak uncontrolled paths or secrets.
- Documentation: clearly separate absent, nil, defaulted, and fixed values.
