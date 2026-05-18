# TASK-0036: document-oriented-open-content-planning

Status: accepted.

Task ID: `TASK-0036`
Gate: `0.5.0` Document-Oriented and Open Content planning; starts only after `TASK-0035` is accepted.
Requirement IDs: designed `REQ-SCHEMA-013`, `REQ-BIND-004`, `REQ-XML-R-002`, `REQ-XML-W-002`, `REQ-VAL-008`, `REQ-GEN-*`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`, `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define accepted wildcard, open-content, mixed-content, unknown XML retention, and canonicalization/serialization policy for `0.5.0`, including generated model shapes, interop fixtures, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0037` through `TASK-0040`
Documentation to update: compatibility profiles, generated-code contract, runtime architecture, conformance matrix, traceability matrix, follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: open-content semantics are decision-complete before implementation; interop candidates are identified; no DOM-first runtime is introduced without ADR
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose document-like fixtures with external validation or serialization comparison where practical.
- Native Image: define open-content fixtures for selected lanes.
- Security: unknown content handling must preserve resolver and parser safety.
- Documentation: avoid implying arbitrary DOM-based binding.

## Accepted `0.5.0` Planning Scope

`TASK-0036` defines planned opt-in profile `XP-XSD10-DOCUMENT` for follow-on implementation tasks.
It composes `XP-XSD10-SEMANTIC` and adds only accepted document-oriented behavior from
`TASK-0037`, `TASK-0038`, and `TASK-0039`. `TASK-0036` itself must not add the public token or
generator behavior.

`TASK-0037` shall add the public `XP-XSD10-DOCUMENT` token across generator API, CLI, Gradle
plugin, and CoreGenerator, then implement only direct `xs:any` particles inside accepted sequences.
Accepted wildcards use `processContents="skip"` and deterministic namespace constraints `##any`,
`##other`, `##local`, `##targetNamespace`, or explicit namespace URI tokens. Accepted wildcard
content binds as immutable `List<XmlFragment>` values using planned dependency-free runtime-core
fragment values. Wildcards in choices, attributes, substitution branches, unsupported group or
derivation edge cases, `xs:anyAttribute`, `processContents="lax"` or `"strict"`, unsupported
namespace constraints, and DOM-backed binding must fail with deterministic diagnostics.

`TASK-0038` shall implement only `mixed="true"` complex types with accepted sequence content.
Generated models preserve text and known element order through a generated sealed content-list
type. Branches represent text, known elements, and accepted wildcard fragments. Mixed choices,
mixed wildcard plus unsupported particles, mixed derivation edge cases, comments or processing
instruction preservation, entity-reference semantics, and DOM-backed binding must fail explicitly.

`TASK-0039` shall define and test stable project serialization for generated writers, retained
unknown XML, namespace prefix assignment, controlled attribute ordering, and text escaping. It must
not claim W3C XML Canonicalization or cryptographic canonical XML compatibility.

## Planned Public Interfaces

`TASK-0037` is expected to add the public profile token `XP-XSD10-DOCUMENT`. It is also expected to
add small dependency-free `runtime-core` values named `XmlFragment`, `XmlFragmentContent`,
`XmlAttribute`, and `XmlNamespaceDeclaration`. These values must be immutable, parser-neutral, safe
for Native Image, and must not wrap DOM, StAX, SAX, XPath, parser handles, mutable maps, or external
resources.

## Planned Verification

Planned test IDs for the implementation sequence are `T-DOCUMENT-PROFILE-*`,
`T-WILDCARD-FRONTEND-*`, `T-WILDCARD-IR-*`, `T-WILDCARD-BIND-*`, `T-WILDCARD-SOURCE-*`,
`T-WILDCARD-READER-*`, `T-WILDCARD-WRITER-*`, `T-WILDCARD-VALIDATOR-*`,
`T-MIXED-CONTENT-*`, `T-SERIALIZATION-POLICY-*`, `T-CONF-XP-XSD10-DOCUMENT-*`, and
`T-INTEROP-DOCUMENT-*`.

Future implementation tasks must cover frontend/profile gating, parser capture, IR, binding,
generated source, reader/writer/validator behavior, unsupported diagnostics, deterministic
generation, generated-source compilation, conformance/interop, security cases for unknown content,
and representative generated-code Native Image smoke.

## Acceptance Evidence

- Added docs-only planned profile `XP-XSD10-DOCUMENT` for the `0.5.0` document-oriented and
  open-content slice without adding the public API token, product source, generated output,
  dependency metadata, build-gate changes, release tags, or publication claims.
- Added designed requirement placeholders for wildcard/open-content schema support, generated
  open-content binding/model shape, mixed-content ordering, retained-fragment reader/writer
  behavior, serialization policy, and validation/security behavior.
- Documented the planned runtime/model shape using dependency-free `runtime-core` values:
  `XmlFragment`, `XmlFragmentContent`, `XmlAttribute`, and `XmlNamespaceDeclaration`; DOM-backed
  binding and parser-handle retention remain out of scope.
- Updated follow-on task cards `TASK-0037` through `TASK-0040` with accepted scope, planned test
  identifiers, unsupported diagnostics, conformance/interop expectations, Native Image
  expectations, and readiness criteria.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
