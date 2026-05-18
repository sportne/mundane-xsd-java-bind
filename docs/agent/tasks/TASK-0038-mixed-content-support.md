# TASK-0038: mixed-content-support

Status: draft.

Task ID: `TASK-0038`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0037` is accepted.
Requirement IDs: designed `REQ-SCHEMA-013`, designed `REQ-BIND-004`, designed `REQ-XML-R-002`, designed `REQ-XML-W-002`, designed `REQ-VAL-008`, `REQ-GEN-*`, `REQ-NI-001`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/conformance/matrix.md`
Target modules: generator/runtime modules approved by `TASK-0036`, conformance tests, examples
Allowed files: mixed-content parser/IR/binding/runtime/emitter/reader/writer/validation source and tests, golden fixtures, interop fixtures, directly related docs
Forbidden files: unapproved DOM-first runtime, XSD 1.1 assertions, dependency metadata unless approved, canonicalization changes reserved for `TASK-0039`
Expected behavior: implement accepted mixed-content support with deterministic generated content-list model shape, read/write ordering semantics, validation behavior, and unsupported diagnostics for out-of-scope mixed-content constructs.
Tests to add/update: `T-MIXED-CONTENT-*`, `T-CONF-XP-XSD10-DOCUMENT-*`, `T-INTEROP-DOCUMENT-*`, golden source tests, generated compile tests, read/write ordering tests, round-trip tests, invalid mixed-content diagnostics, interop comparisons
Documentation to update: generated-code contract, runtime architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted mixed-content fixtures work end to end; ordering semantics are documented and tested; interop evidence is recorded
Rollback notes: revert mixed-content implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare mixed-content fixture validity and serialization behavior where practical.
- Native Image: mixed-content representation remains reflection-free.
- Security: text size and nesting limits remain testable.
- Documentation: describe generated mixed-content model shape explicitly.

## Accepted Scope

- Accept `mixed="true"` only on complex types with already accepted sequence content. The profile
  gate is `XP-XSD10-DOCUMENT`; narrower profiles must reject mixed content explicitly.
- Generate a sealed content-list model named `<ContainingTypeSimpleName>Content`; containing records
  expose an immutable `List<<ContainingTypeSimpleName>Content>` preserving text and element order.
- Generate branch records for text, accepted known elements, and accepted wildcard fragments. Known
  element branches carry the existing bound value type; wildcard branches carry `XmlFragment`.
- Readers preserve source order for non-whitespace text and accepted child elements. Whitespace-only
  text handling must be deterministic and documented in the generated-code contract before
  acceptance.
- Writers serialize content-list items in list order. Validators check known element cardinality and
  accepted wildcard constraints through the generated content list.
- Reject mixed choices, mixed content on unsupported complex shapes, mixed derivation edge cases,
  comments or processing instruction preservation, entity-reference semantics, DOM-backed binding,
  parser-handle retention, and broad serializer rewrites reserved for `TASK-0039`.
