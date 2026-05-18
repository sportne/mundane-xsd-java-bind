# TASK-0033: substitution-groups

Status: draft.

Task ID: `TASK-0033`
Gate: `0.4.0` XSD 1.0 Semantic Expansion; starts only after `TASK-0032` is accepted.
Requirement IDs: designed `REQ-SCHEMA-012`, designed `REQ-BIND-003`, `REQ-MODEL-*`, designed `REQ-VAL-007`, `REQ-GEN-*`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0031`
Allowed files: parser/IR/binding/emitter/reader/writer/validation source and tests for accepted substitution group behavior; golden fixtures; interop fixtures; directly related docs
Forbidden files: full polymorphism beyond accepted substitution-group scope, wildcards, mixed content, dependency metadata
Expected behavior: implement accepted direct substitution group support with deterministic generated sealed branch representation, reader dispatch, writer output, validation behavior, and explicit diagnostics for unsupported substitution group cases.
Tests to add/update: `T-SUBSTITUTION-*`, golden IR/binding/source tests, generated compile tests, valid/invalid substitution tests, round-trip tests, unsupported diagnostics, conformance fixtures, interop comparisons, and representative generated-code Native Image smoke coverage
Documentation to update: generated-code contract, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted substitution group fixtures work end to end; unsupported cases are diagnosed; interop evidence is recorded
Rollback notes: revert substitution group implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare representative substitution group fixtures with external validation where practical.
- Native Image: generated dispatch must be explicit and statically reachable.
- Security: substitution graph resolution must be bounded.
- Documentation: describe generated polymorphic shape before claiming support.

## Accepted Implementation Scope

- Accept direct global `xs:element substitutionGroup="head"` members only.
- Accept singleton references to accepted substitution group heads only.
- Generate an explicit sealed branch type with one record branch per accepted head or member element,
  preserving the actual XML element name for generated reader/writer dispatch.
- Resolve substitution group heads and members before binding; generated code must not perform
  runtime reflection or classpath scanning.
- Reject nested substitution groups, cycles, blocking/final semantics, wildcards, mixed content,
  repeated substitution groups, full polymorphism, abstract complex types, missing heads,
  unsupported member types, duplicate member element names, and unsupported substitution depth with
  deterministic diagnostics.
- Keep identity constraints, full derivation semantics, and document-oriented open content out of
  scope.
