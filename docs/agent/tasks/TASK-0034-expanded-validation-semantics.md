# TASK-0034: expanded-validation-semantics

Status: draft.

Task ID: `TASK-0034`
Gate: `0.4.0` XSD 1.0 Semantic Expansion; starts only after `TASK-0033` is accepted.
Requirement IDs: designed `REQ-SCHEMA-011`, designed `REQ-SCHEMA-012`, designed `REQ-BIND-003`, designed `REQ-VAL-007`, `REQ-GEN-*`, `REQ-QA-001`
ADR IDs: `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/verification/verification-plan.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, `modules/runtime-core` if accepted validation primitives require it, conformance tests, examples
Allowed files: validation plan/emitter/runtime primitive source and tests for accepted `0.4.0` semantics; golden fixtures; interop fixtures; directly related docs
Forbidden files: identity constraints unless separately accepted, XSD 1.1 assertions, wildcards/mixed content, dependency metadata
Expected behavior: expand generated validation only for accepted `0.4.0` features, keeping diagnostics deterministic, location-aware where possible, and explicit for unsupported validation behavior.
Tests to add/update: `T-SEMANTIC-VALIDATION-*`, generated validation source tests, positive/negative semantic validation tests, diagnostic ordering tests, conformance fixtures, interop comparisons, and representative Native Image validation fixtures if selected
Documentation to update: validation architecture, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted semantic validation behavior is generated and tested; unsupported validation remains explicit; interop evidence is recorded
Rollback notes: revert validation implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted validation outcomes against approved XML Schema validation where practical.
- Native Image: validation paths must remain reflection-free.
- Security: diagnostic path sanitization remains enforced.
- Documentation: validation coverage must be feature-specific.

## Accepted Implementation Scope

- Generate validation for accepted nil content rules, including diagnostics for non-empty nil
  elements and unsupported nil/default/fixed combinations.
- Generate fixed-value checks for object validation and XML validation after reader lexical
  conversion.
- Preserve generated reader behavior for effective attribute default/fixed values and absent
  optional elements.
- Validate substitution group dispatch using the resolved actual element name and generated sealed
  branch type.
- Preserve deterministic diagnostic ordering and location-aware XML diagnostics where generated
  readers can retain location.
- Reject identity constraints, XSD 1.1 assertions, wildcards, mixed content, full datatype
  semantics, full derivation semantics, and unsupported validation categories with deterministic
  schema diagnostics rather than partial runtime behavior.
