# TASK-0028: simple-type-composition

Status: accepted.

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

## Acceptance Evidence

Implemented in this task:

- Added normalized IR branches for named `xs:list` and `xs:union` simple types in
  `XP-XSD10-COMPOSED`, while default, choice, and basic-validation profiles keep deterministic
  unsupported-profile diagnostics for list/union constructs.
- Bound accepted list-valued required singleton elements and required attributes as immutable
  `List<T>` generated model components, with explicit generated reader tokenization, writer
  space-joined serialization, and validator item/facet checks.
- Bound accepted union-valued elements and attributes as lexical `String` generated model
  components, with explicit generated validator checks for member parser/facet alternatives.
- Added generator-core frontend, IR, binding, CoreGenerator, generated-source compile, unsupported
  diagnostic, deterministic, and conformance fixture coverage for accepted and rejected list/union
  shapes.
- Expanded the `XP-XSD10-COMPOSED` conformance fixture to compare list item and union validation
  outcomes with JDK XML Schema validation and generated bindings.

Verification run:

- `./gradlew :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `JAVA_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 GRAALVM_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 PATH=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/lib/svm/bin:$PATH ./gradlew :modules:generator-core:generatedCodeNativeSmoke --console=plain`
- `./gradlew nativeSmoke --console=plain` was attempted with the default Java 21 toolchain and
  blocked because `/usr/lib/jvm/java-21-openjdk-amd64/bin/native-image` is unavailable.
- `JAVA_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 GRAALVM_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 PATH=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/lib/svm/bin:$PATH ./gradlew nativeSmoke --console=plain` built the generated-code native executable, then failed in `:modules:runtime-core:nativeTestCompile` because the GraalVM build plugin invoked a non-executable Gradle-managed `bin/native-image` stub.
