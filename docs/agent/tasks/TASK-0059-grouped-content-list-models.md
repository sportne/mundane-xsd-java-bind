# TASK-0059: grouped-content-list-models

Status: draft.

Task ID: `TASK-0059`
Gate: grouped content-list model support for the 1.0.0 full-XSD sequence.
Target areas: compiler IR, binding model, generated model/reader/writer/validator emitters,
generator-core tests, and selected conformance fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for generated shape/verification updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: support repeated/optional multi-particle groups, optional `xs:all` groups with
required children, mixed choices, and wildcard choices using generated sealed content-list models
when field flattening cannot represent the content safely.
Tests to add/update: unit tests for particle normalization and binding-shape decisions; generated
reader/writer/validator tests for valid and invalid grouped content; selected JDK XML Schema
comparison fixtures for supported shapes.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: previously diagnostic grouped content shapes in this task's scope generate
stable public models, preserve source/writer order where applicable, validate cardinality/order
deterministically, and do not broaden unsupported full-XSD claims.
Rollback notes: revert grouped-content IR/binding/emitter changes, fixtures, docs, and task status.
