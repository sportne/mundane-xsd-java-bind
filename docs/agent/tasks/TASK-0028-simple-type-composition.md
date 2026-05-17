# TASK-0028: simple-type-composition

Status: draft.

Task ID: `TASK-0028`
Gate: `0.3.0` Composed XSD 1.0 Schemas; starts only after `TASK-0027` is accepted.
Requirement IDs: planned `REQ-SCHEMA-009`, planned `REQ-VAL-005`, `REQ-GEN-*`, `REQ-XML-W-001`, `REQ-XML-R-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0026`
Allowed files: simple type parser/IR/binding/validation/emitter source and tests for accepted list/union behavior; golden fixtures; interop fixtures; directly related docs
Forbidden files: full datatype system beyond accepted scope, XSD 1.1 assertions, identity constraints, dependency metadata, runtime dependency additions
Expected behavior: implement `XP-XSD10-COMPOSED` list and union support for the accepted `TASK-0026` subset: named `xs:list` simple types with supported scalar or restricted scalar alias `itemType`, singleton element/attribute binding as `List<T>`, named `xs:union` simple types with supported scalar or restricted scalar alias `memberTypes`, and lexical `String` union binding with generated member validation.
Tests to add/update: `T-LIST-*` and `T-UNION-*` frontend, IR, binding, source, generated compile, reader/writer/validator, unsupported diagnostic, deterministic emission, round-trip, conformance, and interop comparisons
Documentation to update: validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance checks, `git diff --check`
Acceptance criteria: accepted list/union fixtures work end to end; unaccepted cases fail explicitly; interop evidence is recorded where practical
Rollback notes: revert simple type composition implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare list/union validation outcomes with approved XML Schema validators where practical.
- Native Image: generated validators must remain explicit.
- Security: lexical handling must avoid unbounded input behavior.
- Documentation: support claims must not imply complete XSD datatype support.

## Accepted Implementation Shape

- Use whitespace tokenization for accepted `xs:list` values and reject invalid item lexical values
  through existing reader/validator diagnostic paths.
- Bind accepted list-valued singleton elements and attributes as immutable `List<T>` record
  components; do not implement list cardinality composition beyond the singleton XSD value shape in
  this task.
- Bind accepted `xs:union` values as `String` so generated models avoid a new public variant type;
  generated validators must check that at least one accepted member parser/facet rule matches.
- Reject anonymous list/union member types, unsupported member bases, nested list/union composition,
  and full datatype semantics with deterministic diagnostics.
