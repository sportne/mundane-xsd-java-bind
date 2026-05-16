# TASK-0023: xs-choice-support

Status: draft.

Task ID: `TASK-0023`
Gate: `0.2.0` Practical Data Contracts; starts only after `TASK-0022` is accepted.
Requirement IDs: `REQ-SCHEMA-007`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-MODEL-001`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-VAL-002`
ADR IDs: `ADR-0004`, `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0022`
Allowed files: schema frontend/IR/binding/emitter/validation source and tests needed for accepted `xs:choice` shapes, golden fixtures, conformance fixtures, example fixtures, and directly related docs
Forbidden files: unsupported choice shapes, full model-group implementation beyond the accepted `0.2.0` scope, dependency metadata, runtime dependency additions, CLI or Gradle plugin behavior changes not required to expose existing generation paths
Expected behavior: implement feasible `xs:choice` support through parsing, IR, binding model, generated model shape, reader/writer behavior, validation diagnostics, deterministic golden output, round trips, and explicit unsupported diagnostics for out-of-scope choices.
Tests to add/update: golden IR/binding/source tests, generated compile tests, valid and invalid reader/writer tests, cardinality tests, round-trip tests, unsupported-choice diagnostics, Native Image smoke fixtures if selected, and interop fixtures where JDK/XSD validation can act as a reference
Documentation to update: conformance matrix, compatibility profiles, generated-code contract if model shape changes, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks named by the implementation, `git diff --check`
Acceptance criteria: accepted `xs:choice` fixtures generate deterministic, compileable, round-tripping code; out-of-scope choices produce deterministic diagnostics; interop evidence is recorded where practical
Rollback notes: revert choice implementation, tests, fixtures, golden outputs, and directly related docs

## Impact Notes

- Interop: include at least one positive and one negative choice fixture compared against an approved XML Schema validator where practical.
- Native Image: choice-generated reader/writer paths must remain reflection-free.
- Security: no new XML resource access behavior is introduced.
- Documentation: do not imply full model-group support.
