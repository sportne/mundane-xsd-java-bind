# TASK-0061: derivation-dynamic-typing-completion

Status: accepted.

Task ID: `TASK-0061`
Gate: complete derivation, restriction, block/final, and dynamic typing.
Target areas: component graph, IR derivation metadata, binding polymorphism, generated
reader/writer/validator dispatch, generator-core tests, and conformance fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for generated shape/validation updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: implement accepted XSD 1.0 derivation semantics needed by generated bindings:
preserved derivation metadata, abstract complex type metadata, final/block checks for accepted
derivation/substitution paths, declared-base polymorphism, and known `xsi:type` dispatch.
Tests to add/update: unit tests for legal/illegal extension and restriction chains; block/final and
abstract use diagnostics; generated reader/writer/validator tests for substitution and `xsi:type`
dispatch; selected JDK XML Schema comparison fixtures.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: dynamic values are accepted only when legal by derivation/substitution rules,
invalid abstract/blocked/final/unknown `xsi:type` cases fail deterministically, and public generated
polymorphic shapes are documented before the full profile is enabled.
Rollback notes: revert derivation/polymorphism changes, fixtures, docs, and task status.

## Completion evidence

- Added IR metadata for abstract complex types, derivation base/kind, and effective block/final
  controls while keeping `XP-XSD10-FULL` non-executable.
- Added binding metadata for declared complex-base element fields with known concrete derived
  candidates. Generated model APIs use sealed `xsiType` branch records; generated readers dispatch
  known `xsi:type` values, writers emit `xsi:type` for derived branch values, and validators recurse
  through concrete branch values.
- Added deterministic diagnostics for blocked substitution heads, final derivation bases, and
  unknown generated `xsi:type` values. Root-element `xsi:type` dispatch remains out of this
  accepted generated API shape and is tracked as a later full-profile edge.
- Addressed review findings by enforcing complex-type `block` controls for accepted dynamic-type
  branches, stopping dynamic block checks at the declared base type, and rejecting invalid
  `block`/`final` control tokens deterministically.
- Added selected JDK XML Schema comparison fixture
  `T-CONF-XP-XSD10-SEMANTIC-XSI-TYPE`.

Verification evidence:

- `./gradlew :modules:generator-core:check --console=plain` passed.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
