# TASK-0060: content-model-automata-upa

Status: draft.

Task ID: `TASK-0060`
Gate: complete content-model automata and UPA validation.
Target areas: normalized particle compiler, reader/validator execution plans, UPA diagnostics,
generator-core tests, and conformance fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for validation architecture and feature-matrix updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: compile XSD 1.0 particles into a deterministic automaton reused by generated
readers and object validators; enforce complete UPA diagnostics before binding/source emission.
Tests to add/update: automata unit tests for nested sequence/choice/all/group/wildcard
compositions; reader and validator agreement tests; UPA conflict tests for element/element,
element/wildcard, and wildcard/wildcard overlap; selected JDK XML Schema comparison fixtures.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: reader and validator consume the same compiled content-model semantics, UPA
diagnostics are deterministic, accepted shapes match JDK XML Schema behavior where it is a useful
oracle, and `XP-XSD10-FULL` remains non-executable.
Rollback notes: revert automata/compiler changes, fixtures, docs, and task status.
