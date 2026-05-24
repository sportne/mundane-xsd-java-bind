# TASK-0060: content-model-automata-upa

Status: accepted.

Task ID: `TASK-0060`
Gate: accepted content-model automata and UPA validation.
Target areas: normalized particle compiler, reader/validator execution plans, UPA diagnostics,
generator-core tests, and conformance fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for validation architecture and feature-matrix updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: compile accepted XSD 1.0 particles into deterministic grouped-content plans
reused by generated readers and object validators; enforce deterministic UPA diagnostics for the
accepted element/wildcard and wildcard/wildcard conflict surface before binding/source emission.
Tests to add/update: automata unit tests for nested sequence/choice/all/group/wildcard
compositions; reader and validator agreement tests; UPA conflict tests for element/element,
element/wildcard, and wildcard/wildcard overlap; selected JDK XML Schema comparison fixtures.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: reader and validator consume the same grouped-content position semantics, UPA
diagnostics are deterministic for accepted conflict shapes, accepted shapes match JDK XML Schema
behavior where it is a useful oracle, and `XP-XSD10-FULL` remains non-executable.
Rollback notes: revert automata/compiler changes, fixtures, docs, and task status.

Implementation notes:

- Added content-group position metadata so generated readers and validators consume the same
  deterministic grouped-content plan instead of treating every branch as a distinct sequence step.
- Accepted repeated/optional grouped sequences and group refs with nested choice positions that were
  previously deferred after `TASK-0059`.
- Added deterministic UPA diagnostics for accepted wildcard/wildcard overlap alongside existing
  element/wildcard overlap checks. Remaining full-suite UPA row mapping stays with `TASK-0064`.
- Kept `XP-XSD10-FULL` non-executable. Full derivation/dynamic typing, strict/lax wildcard deep
  validation, remaining datatype/nil/identity edges, W3C generated-binding mapping, and release
  workflow work remain later gates.

Verification evidence:

- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
