# TASK-0016: basic-validation-and-diagnostics

Status: draft.

Task ID: `TASK-0016`
Gate: Phase 4 generated reader and basic validation vertical slice; starts only after `TASK-0015` is accepted.
Requirement IDs: `REQ-VAL-001`, `REQ-VAL-002`, `REQ-VAL-003`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-GEN-001`, `REQ-RT-001`, `REQ-RT-002`, `REQ-QA-001`
ADR IDs: `ADR-0004`, `ADR-0005`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/architecture/generated-code-contract.md`, `docs/verification/verification-plan.md`
Target module: `modules/generator-core` with accepted `runtime-core` validation types
Allowed files: generator-core validation-plan/emitter source/tests/resources/golden fixtures, generated validation behavior fixtures, and directly related docs
Forbidden files: public generator API source, CLI source, Gradle plugin source, new runtime dependencies, unsupported full-facet validation, identity constraints, default/fixed full semantics, XSD 1.1 assertions, and committed generated product code outside approved golden fixtures
Expected behavior: emit deterministic basic validation support for required fields, sequence order, cardinality, common primitive lexical conversions, and stable diagnostics with locations where available; unsupported validation features must produce explicit profile diagnostics.
Tests to add/update: generated validation golden source tests, valid/invalid sequence tests, required/optional/repeated cardinality tests, primitive lexical diagnostics, unsupported facet diagnostics, deterministic diagnostic ordering tests, and generated compile tests
Documentation to update: validation architecture, conformance matrix, traceability, and verification plan when validation rows become implemented
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: basic validation behavior is generated and tested for phase-one constructs; future validation semantics remain profile-gated; no CLI or Gradle entry point is added
Rollback notes: revert validation emitter/source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: negative tests are required for each implemented validation class.
- Native Image: validation code must be explicit and reflection-free.
- Security: diagnostics must avoid leaking local secrets or uncontrolled full paths.
- Documentation: validation claims must stay limited to implemented profile behavior.
