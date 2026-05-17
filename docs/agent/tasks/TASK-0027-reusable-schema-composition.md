# TASK-0027: reusable-schema-composition

Status: accepted.

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

## Acceptance Evidence

Implemented in this task:

- Added public profile `XP-XSD10-COMPOSED` across generator API, CoreGenerator, CLI help/parsing,
  and Gradle plugin profile handling.
- Frontend parsing now accepts `xs:group` and `xs:attributeGroup` only under
  `XP-XSD10-COMPOSED`; the default and narrower profiles keep deterministic unsupported-profile
  diagnostics.
- Normalized IR records named model groups and attribute groups, resolves accepted refs, flattens
  group particles and attribute-group attributes before binding, and rejects unsupported shapes with
  deterministic diagnostics.
- Binding and generated source reuse the flattened field/attribute model; generated records,
  readers, writers, and validators require no new runtime binding mechanism.
- Added positive/negative composed conformance fixtures compared against JDK XML Schema validation
  and generated bindings.
- Added representative composed generated-code smoke coverage for the Native Image smoke lane.

Verification run:

- `./gradlew :modules:generator-api:check :modules:generator-cli:check :modules:generator-gradle-plugin:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `./gradlew nativeSmoke --console=plain` was attempted locally but blocked because
  `/usr/lib/jvm/java-21-openjdk-amd64/bin/native-image` is unavailable. The generated-code smoke
  fixture for the composed path is included, and CI/GraalVM native-lane expectations remain
  unchanged.

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
