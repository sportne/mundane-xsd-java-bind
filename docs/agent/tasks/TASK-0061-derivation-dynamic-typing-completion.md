# TASK-0061: derivation-dynamic-typing-completion

Status: draft.

Task ID: `TASK-0061`
Gate: complete derivation, restriction, block/final, and dynamic typing.
Target areas: component graph, IR derivation metadata, binding polymorphism, generated
reader/writer/validator dispatch, generator-core tests, and conformance fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for generated shape/validation updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: implement full XSD 1.0 complex/simple derivation semantics needed by generated
bindings: complete restriction algebra, block/final/default semantics, abstract complex type rules,
declared-base polymorphism, and known `xsi:type` dispatch.
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
