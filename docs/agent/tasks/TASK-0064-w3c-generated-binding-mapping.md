# TASK-0064: w3c-generated-binding-mapping

Status: draft.

Task ID: `TASK-0064`
Gate: W3C XML Schema 1.0 generated-binding mapping and execution.
Target areas: W3C suite intake, classification manifest/reporting, generated-binding execution
harness, conformance tests, docs, and traceability.
Allowed files: conformance-tests production/tests/resources, generator-core test fixtures when
needed, docs for conformance/verification updates, and task evidence.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, vendored W3C
suite contents, XSD 1.1, XML 1.1, canonical XML, DOM-backed binding, or enabling
`XP-XSD10-FULL`.
Expected behavior: map every in-scope W3C XML Schema 1.0 row to generated-binding execution or to a
documented non-goal/product-scope-incompatible category; execute binding-supported rows through
generate, compile, read, validate, write, re-read, and semantic comparison.
Tests to add/update: parser/classifier tests for stable row mapping; generated-binding harness
tests with local W3C-shaped fixtures; full opt-in W3C lane execution against the pinned local suite.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: the W3C report has no unclassified rows, no accidental XSD 1.1/XML 1.1 rows,
all generated-binding-supported rows pass executable checks, and remaining non-executable rows have
accepted product-scope rationale.
Rollback notes: revert W3C mapping/harness changes, report policy docs, fixtures, and task status.
