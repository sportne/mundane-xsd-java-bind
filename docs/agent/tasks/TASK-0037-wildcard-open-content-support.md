# TASK-0037: wildcard-open-content-support

Status: draft.

Task ID: `TASK-0037`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0036` is accepted.
Requirement IDs: future accepted wildcard/open-content schema, binding, XML reader/writer, validation, generation, and security IDs
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/conformance/matrix.md`
Target modules: generator/runtime modules approved by `TASK-0036`, conformance tests, examples
Allowed files: wildcard/open-content parser/IR/binding/runtime/emitter/reader/writer/validation source and tests, golden fixtures, interop fixtures, directly related docs
Forbidden files: DOM-first generated runtime unless approved by ADR, mixed content unless accepted in `TASK-0038`, dependency metadata unless approved, XSD 1.1 assertions
Expected behavior: implement accepted wildcard/open-content support with explicit generated representation, controlled unknown XML handling, deterministic serialization, validation behavior, and unsupported diagnostics for out-of-scope wildcards.
Tests to add/update: golden source tests, generated compile tests, open-content reader/writer tests, validation tests, security tests for unknown content handling, interop comparisons, round trips
Documentation to update: generated-code contract, runtime architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted wildcard/open-content fixtures work end to end; security constraints hold; interop evidence is recorded
Rollback notes: revert wildcard/open-content implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted wildcard fixtures with external validation and serialization expectations where practical.
- Native Image: unknown content representation must be statically reachable.
- Security: unknown content must not enable uncontrolled resource access.
- Documentation: clearly define retained versus rejected unknown content.
