# TASK-0063: datatype-nil-identity-edges

Status: draft.

Task ID: `TASK-0063`
Gate: remaining datatype, nil, and identity-validation edges.
Target areas: datatype/list/union composition, NOTATION semantics, `xsi:nil` interactions,
identity-constraint validation, generator-core tests, runtime-core tests where needed, and
conformance fixtures.
Allowed files: runtime-core and generator-core production/tests, conformance fixtures/tests,
generated-code smoke fixtures, and docs for datatype/validation updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: close remaining XSD 1.0 validation edges needed before W3C generated-binding row
mapping, including anonymous/nested list and union composition, NOTATION value semantics,
`xsi:nil` interactions with defaults/fixed/derivation/cardinality, and identity-constraint edge
cases for generated model shapes.
Tests to add/update: runtime datatype unit tests; generator syntax/IR/binding/emitter tests;
generated reader/writer/validator tests for nil and identity interactions; selected JDK XML Schema
comparison fixtures.
Commands to run: `./gradlew :modules:runtime-core:check :modules:generator-core:check
:modules:conformance-tests:check --console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke
--console=plain`, `./gradlew validateDesignControlPack qualityGate --console=plain`,
`git diff --check`.
Acceptance criteria: all feature-matrix datatype/nil/identity blockers needed for W3C mapping are
either supported with tests or explicitly reclassified as product-scope-incompatible/non-goals with
accepted rationale; `XP-XSD10-FULL` remains non-executable.
Rollback notes: revert datatype/nil/identity changes, fixtures, docs, and task status.
