# TASK-0039: canonicalization-serialization-policy

Status: accepted.

Task ID: `TASK-0039`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0038` is accepted.
Requirement IDs: designed `REQ-XML-W-002`, designed `REQ-XML-R-002`, `REQ-GEN-*`, designed `REQ-VAL-008`, `REQ-NI-001`
ADR IDs: `ADR-0005`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/verification/conformance-strategy.md`
Target modules: generator-core, runtime modules if accepted, conformance tests, examples
Allowed files: serialization policy tests/source for accepted behavior, golden XML/source fixtures, interop fixtures, directly related docs
Forbidden files: cryptographic XML canonicalization claims unless separately approved, broad serializer rewrites outside accepted policy, dependency metadata unless approved
Expected behavior: add accepted serialization-policy tests for generated XML output, including namespace prefix policy, controlled attribute ordering, text handling, open/mixed content serialization, and stable interop expectations without formal XML Canonicalization claims.
Tests to add/update: `T-SERIALIZATION-POLICY-*`, `T-CONF-XP-XSD10-DOCUMENT-*`, `T-INTEROP-DOCUMENT-*`, XML output golden tests, round-trip normalization tests, interop serialization comparisons where practical, negative tests for unsupported canonicalization claims
Documentation to update: generated-code contract, runtime architecture, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: serialization behavior is stable and documented; no unsupported canonical XML claim is made; interop evidence is recorded
Rollback notes: revert serialization policy tests/source, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare generated XML with approved tool output where useful without requiring byte-identical lexical forms unless accepted.
- Native Image: serialization policy must work in native lanes selected for the slice.
- Security: output behavior must not leak local diagnostic or resolver paths.
- Documentation: distinguish stable project output from formal XML canonicalization.

## Acceptance Evidence

- Added executable serialization-policy coverage for `runtime-jdkxml` output prefix assignment,
  namespaced attributes, text/attribute escaping, secure reparse behavior, and absence of local path
  leakage in serialized XML.
- Added generated-writer coverage proving schema-owned attributes emit before child content, mixed
  content emits in content-list order, retained `XmlFragment` attributes and nested content emit in
  stored list order, and generated writer source keeps forbidden generated-code mechanisms out.
- Added `XP-XSD10-DOCUMENT` conformance serialization checks for accepted wildcard and mixed-content
  fixtures. The checks read valid XML through generated readers, write it through generated writers,
  validate the serialized XML with JDK XML Schema, reparse through the secure adapter, and compare
  generated binding values rather than formal canonical bytes.
- Added a representative generated-code smoke path for mixed/open-content serialization so the
  generated-code Native Image smoke lane compiles the accepted document serialization shape when a
  GraalVM native-image toolchain is available.
- Documented the stable project serialization contract and explicit non-claims for W3C XML
  Canonicalization, exclusive canonicalization, XML Signature canonical forms, cryptographic byte
  stability, lexical prefix preservation, comments, processing instructions, DTDs, and
  entity-reference identity.
- Verification completed:
  - `./gradlew :modules:runtime-jdkxml:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
  - `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`
  - `./gradlew validateDesignControlPack qualityGate --console=plain`
  - `git diff --check`
  - `native-image` was not available on `PATH`; local generated-code Native Image execution remains
    blocked by the local GraalVM toolchain.

## Accepted Scope

- Define stable project XML serialization for generated writers, retained `XmlFragment` values, and
  mixed-content lists under `XP-XSD10-DOCUMENT`.
- Verify namespace prefix assignment remains deterministic for generated and retained unknown
  content. Retained namespace declarations required by unknown fragments must be emitted without
  leaking parser-specific state.
- Verify controlled attribute ordering where project-owned writers control the output. Attribute
  order for retained unknown content must be deterministic after capture, not dependent on mutable
  parser state.
- Verify text escaping for scalar values, mixed text, and retained fragment text.
- Add negative tests and documentation that the project does not claim W3C XML Canonicalization,
  exclusive canonicalization, XML Signature canonical forms, cryptographic byte stability, or
  preservation of comments, processing instructions, DTDs, entity references, or lexical prefix
  choices not represented in runtime-core values.
