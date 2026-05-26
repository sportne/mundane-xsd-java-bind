# TASK-0089: validator-traversal-identity-plan

Status: accepted.

Task ID: `TASK-0089`
Priority: P2
Gate: deeper generator architecture refactor.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-VAL-002`, `REQ-QA-002`
ADR IDs: `ADR-0004`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`,
`docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: validator emitter package-private plan/helper classes and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public API changes, runtime dependency changes, identity semantics changes,
generated source behavior changes, release metadata, dependency metadata, and quality-gate
weakening.
Expected behavior: add package-private validator traversal and identity-helper plan objects before
validator source text assembly while preserving object/XML validation behavior and diagnostics.
Tests to add/update: focused validator/identity plan tests plus existing generated-source
compile/smoke coverage.
Documentation to update: complexity review, generated-code contract or validation architecture if
plan vocabulary changes, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: validator traversal and identity helper decisions have internal plan coverage;
generated validator behavior and diagnostics are unchanged.
Rollback notes: revert validator plan helpers, tests, and docs.

## Completion notes

`TASK-0089` accepted the validator traversal and identity planning tranche. Package-private
`GeneratedValidatorTraversalPlan` now captures validator field order plus element, choice, content,
and branch traversal inputs before source assembly. `GeneratedValidatorIdentityPlan` captures root
identity constraint activation and constraint ordering inputs before identity-helper source
assembly. Generated validator behavior, diagnostics, identity semantics, public APIs, and runtime
dependencies remain unchanged.

## Evidence

- `./gradlew :modules:generator-core:test --tests 'io.github.mundanej.mxjb.generator.core.emit.GeneratedValidatorPlanTest' --console=plain`
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
