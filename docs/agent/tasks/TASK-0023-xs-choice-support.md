# TASK-0023: xs-choice-support

Status: accepted.

Task ID: `TASK-0023`
Gate: `0.2.0` Practical Data Contracts; starts only after `TASK-0022` is accepted.
Requirement IDs: `REQ-SCHEMA-007`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-MODEL-001`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-VAL-002`
ADR IDs: `ADR-0004`, `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0022`
Allowed files: schema frontend/IR/binding/emitter/validation source and tests needed for accepted `xs:choice` shapes, golden fixtures, conformance fixtures, example fixtures, and directly related docs
Forbidden files: unsupported choice shapes, full model-group implementation beyond the accepted `0.2.0` scope, dependency metadata, runtime dependency additions, CLI or Gradle plugin behavior changes not required to expose existing generation paths
Expected behavior: implement the accepted `TASK-0022` `XP-DATA-10-CHOICE` scope through parsing, IR, binding model, generated sealed choice model shape, reader/writer behavior, validation diagnostics, deterministic generated output, round trips, and explicit unsupported diagnostics for out-of-scope choice shapes.
Tests to add/update: golden frontend/IR/binding/source tests, generated compile tests, valid and invalid reader/writer tests, required and optional choice cardinality tests, surrounding sequence-order tests, round-trip tests, unsupported-choice diagnostics, representative Native Image smoke fixtures, and interop fixtures where JDK XML Schema validation can act as a reference
Documentation to update: conformance matrix, compatibility profiles, generated-code contract if model shape changes, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks named by the implementation, `git diff --check`
Acceptance criteria: accepted `xs:choice` fixtures generate deterministic, compileable, round-tripping code; out-of-scope choices produce deterministic diagnostics; interop evidence is recorded where practical
Rollback notes: revert choice implementation, tests, fixtures, golden outputs, and directly related docs

## Impact Notes

- Interop: include at least one positive and one negative choice fixture compared against an approved XML Schema validator where practical.
- Native Image: choice-generated reader/writer paths must remain reflection-free.
- Security: no new XML resource access behavior is introduced.
- Documentation: do not imply full model-group support.

## Accepted `TASK-0022` Scope

Implement only local `xs:choice` particles in a complex type content model, either as the only
content particle or as an item in an enclosing supported `xs:sequence`. Choice particles may use
`minOccurs="0"` or `minOccurs="1"` and must use `maxOccurs="1"`.

Accepted branches are local `xs:element` declarations or references to global elements whose
resolved type is already supported by `XP-DATA-10`. Branch elements are singleton branches; repeated
branch elements, nested `xs:choice`, nested `xs:sequence`, `xs:all`, named model groups,
wildcards, substitution groups, mixed content, anonymous branch complex types, and repeated choices
must continue to produce deterministic unsupported-profile diagnostics.

Generated model binding creates one field per choice particle. The field type is a generated sealed
interface named `<ContainingTypeSimpleName>Choice`; each branch is a generated record named
`<BranchElementSimpleName>Choice` carrying the existing scalar or model value for that branch.
Optional choices use `Optional<<ContainingTypeSimpleName>Choice>`.

Generated readers accept exactly one branch for required choices and zero or one branch for optional
choices, preserve sequence order around surrounding particles, and report deterministic diagnostics
for missing, repeated, out-of-order, or unknown branch elements. Generated writers emit the selected
branch in binding order and must not add runtime dependencies or reflective dispatch.

Planned test identifiers are `T-CHOICE-FRONTEND-*`, `T-CHOICE-IR-*`, `T-CHOICE-BIND-*`,
`T-CHOICE-MODEL-*`, `T-CHOICE-WRITER-*`, `T-CHOICE-READER-*`, `T-CHOICE-VALIDATOR-*`,
`T-RT-CHOICE-*`, `T-CONF-XP-DATA-10-CHOICE-*`, and `T-INTEROP-CHOICE-*`.

## Acceptance Evidence

- Public API exposes `GeneratorProfile.XP_DATA_10_CHOICE` with CLI/Gradle token
  `XP-DATA-10-CHOICE`; `XP-DATA-10` remains the default.
- Syntax, IR, and binding model tests cover the accepted local singleton choice shape and preserve
  unsupported-profile behavior for default `XP-DATA-10`.
- Generated model tests cover sealed choice interfaces and branch records; generated reader, writer,
  and validator tests execute scalar and model branch paths, repeated-choice diagnostics, and
  deterministic generated-source compilation.
- CoreGenerator, CLI, and Gradle plugin tests verify the opt-in profile generates choice sources,
  while default profile input still rejects `xs:choice`.
- Conformance and interop fixtures cover a positive domestic choice document and a negative repeated
  branch document against JDK XML Schema validation and generated reader/writer/validator behavior.
- The generated-code smoke fixture set includes a representative choice model, reader, writer, and
  validator path so the Native Image smoke lane exercises choice-generated code when `native-image`
  is available.
- Targeted verification passed:
  `:modules:generator-core:check`,
  `:modules:generator-api:check`,
  `:modules:generator-cli:check`,
  `:modules:generator-gradle-plugin:check`, and
  `:modules:conformance-tests:check`.
- Repository verification passed with `./gradlew validateDesignControlPack qualityGate` and
  `git diff --check`; local Native Image execution remains dependent on a GraalVM toolchain with
  `native-image` installed.
