# TASK-0027: reusable-schema-composition

Status: draft.

Task ID: `TASK-0027`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0026` is accepted.
Requirement IDs: planned `REQ-SCHEMA-008`, planned `REQ-BIND-001`, `REQ-GEN-*`, `REQ-XML-W-001`, `REQ-XML-R-001`, planned `REQ-VAL-004`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: parser/IR/binding/emitter/validation source and tests for accepted named model group and attribute group behavior; golden fixtures; conformance fixtures; directly related docs
Forbidden files: unapproved derivation, wildcards, substitution groups, mixed content, dependency metadata, runtime dependency additions
Expected behavior: implement `XP-XSD10-COMPOSED` named model group and attribute group support for the accepted `TASK-0026` subset: global `xs:group` declarations with one `xs:sequence` of already-supported particles, singleton direct `xs:group ref` use flattened into containing order, global `xs:attributeGroup` declarations with supported attributes, and direct `xs:attributeGroup ref` use flattened into containing attributes.
Tests to add/update: `T-GROUP-*` and `T-ATTRGROUP-*` frontend, IR, binding, source, generated compile, reader/writer/validator, unsupported diagnostic, deterministic emission, round-trip, conformance, interop, and selected Native Image smoke evidence
Documentation to update: conformance matrix, compiler pipeline docs, traceability matrix, compatibility profiles
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted group constructs work end to end; invalid or unsupported group constructs fail deterministically; interop evidence is recorded
Rollback notes: revert composition implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted group fixtures with external validation where practical.
- Native Image: generated paths must remain reflection-free.
- Security: recursive composition must be bounded and diagnostic-safe.
- Documentation: support claims must name the accepted group subset.

## Accepted Implementation Shape

- Add opt-in profile plumbing only as needed for `XP-XSD10-COMPOSED`; keep default `XP-DATA-10`,
  `XP-DATA-10-CHOICE`, and `XP-VALIDATION-10-BASIC` behavior unchanged.
- Normalize accepted group and attribute-group references by flattening them before binding so
  generated model, reader, writer, and validator shapes remain the existing record/field shape.
- Detect missing references, duplicate flattened field/attribute names, group recursion, repeated or
  optional group refs, nested groups beyond the accepted direct shape, `xs:all`, and wildcards with
  deterministic unsupported diagnostics.
- Add at least one positive and one negative modular-schema fixture compared against JDK XML Schema
  validation and generated bindings.
