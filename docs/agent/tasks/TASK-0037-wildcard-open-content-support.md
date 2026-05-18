# TASK-0037: wildcard-open-content-support

Status: draft.

Task ID: `TASK-0037`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0036` is accepted.
Requirement IDs: designed `REQ-SCHEMA-013`, designed `REQ-BIND-004`, designed `REQ-XML-R-002`, designed `REQ-XML-W-002`, designed `REQ-VAL-008`, `REQ-GEN-*`, `REQ-SEC-001`, `REQ-NI-001`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/conformance/matrix.md`
Target modules: generator/runtime modules approved by `TASK-0036`, conformance tests, examples
Allowed files: wildcard/open-content parser/IR/binding/runtime/emitter/reader/writer/validation source and tests, golden fixtures, interop fixtures, directly related docs
Forbidden files: DOM-first generated runtime unless approved by ADR, mixed content unless accepted in `TASK-0038`, dependency metadata unless approved, XSD 1.1 assertions
Expected behavior: add public `XP-XSD10-DOCUMENT` profile plumbing and implement accepted direct `xs:any` wildcard/open-content support with explicit generated representation, controlled unknown XML handling, deterministic serialization, validation behavior, and unsupported diagnostics for out-of-scope wildcards.
Tests to add/update: `T-DOCUMENT-PROFILE-*`, `T-WILDCARD-FRONTEND-*`, `T-WILDCARD-IR-*`, `T-WILDCARD-BIND-*`, `T-WILDCARD-SOURCE-*`, `T-WILDCARD-READER-*`, `T-WILDCARD-WRITER-*`, `T-WILDCARD-VALIDATOR-*`, `T-CONF-XP-XSD10-DOCUMENT-*`, `T-INTEROP-DOCUMENT-*`, golden source tests, generated compile tests, security tests for unknown content handling, round trips
Documentation to update: generated-code contract, runtime architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted wildcard/open-content fixtures work end to end; security constraints hold; interop evidence is recorded
Rollback notes: revert wildcard/open-content implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare accepted wildcard fixtures with external validation and serialization expectations where practical.
- Native Image: unknown content representation must be statically reachable.
- Security: unknown content must not enable uncontrolled resource access.
- Documentation: clearly define retained versus rejected unknown content.

## Accepted Scope

- Add public `XP-XSD10-DOCUMENT` profile plumbing across generator API, CLI help/parsing, Gradle
  plugin validation, and CoreGenerator. Default, choice, validation-basic, composed, and semantic
  profiles must reject `xs:any` with deterministic unsupported-profile diagnostics.
- Accept direct `xs:any` particles inside already accepted `xs:sequence` content only, after prior
  group/derivation flattening where applicable.
- Accept `processContents="skip"` only. Omitted `processContents` must fail explicitly unless the
  schema says `skip`.
- Accept namespace constraints `##any`, `##other`, `##local`, `##targetNamespace`, and explicit
  namespace URI tokens when they resolve deterministically against the schema target namespace.
- Bind accepted wildcard fields as immutable `List<XmlFragment>`. `TASK-0037` owns adding
  dependency-free runtime-core `XmlFragment`, `XmlFragmentContent`, `XmlAttribute`, and
  `XmlNamespaceDeclaration` values.
- Readers capture retained unknown element subtrees from the project `XmlEventReader`; writers emit
  retained fragments through `XmlOutput`; validators enforce wildcard cardinality and namespace
  constraints without schema-validating retained unknown fragments.
- Reject wildcards in choices, attributes, substitution branches, unsupported group or derivation
  edge cases, `xs:anyAttribute`, `processContents="lax"` or `"strict"`, unsupported namespace
  constraints, comments/PI retention, entity-reference semantics, DOM-backed binding, parser-handle
  retention, and new runtime dependencies.
