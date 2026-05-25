# TASK-0064: w3c-generated-binding-mapping

Status: accepted.

Task ID: `TASK-0064`
Gate: W3C XML Schema 1.0 generated-binding mapping and execution.
Target areas: W3C suite intake, classification manifest/reporting, generated-binding execution
harness, conformance tests, docs, and traceability.
Allowed files: conformance-tests production/tests/resources, generator-core test fixtures when
needed, docs for conformance/verification updates, and task evidence.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, vendored W3C
suite contents, XSD 1.1, XML 1.1, canonical XML, DOM-backed binding, or enabling
`XP-XSD10-FULL`.
Expected behavior: map every W3C XML Schema 1.0 row explicitly selected as in scope for this gate
to generated-binding execution, and keep all other discovered rows classified as validation-only,
tolerated metadata, expected diagnostics, product-scope-incompatible, or blocked with deterministic
rationale; execute binding-supported rows through generate, compile, read, validate, write,
re-read, and semantic comparison.
Tests to add/update: parser/classifier tests for stable row mapping; generated-binding harness
tests with local W3C-shaped fixtures; full opt-in W3C lane execution against the pinned local suite.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: the W3C report has no unclassified rows, no accidental XSD 1.1/XML 1.1 rows,
all explicitly selected generated-binding-supported rows pass executable checks, and remaining
non-executable rows retain deterministic classification rationale without being claimed as full
generated-binding support.
Rollback notes: revert W3C mapping/harness changes, report policy docs, fixtures, and task status.

## Completion notes

`TASK-0064` adds explicit W3C generated-binding mapping evidence for the row subset selected for
this gate without vendoring the W3C suite or enabling `XP-XSD10-FULL`. The opt-in
`w3cXsd10Conformance` lane now classifies mapped rows as `binding-supported`, writes
`binding-executions.tsv`, and executes the mapped generated-binding path through generation, Java
compilation, JDK XML Schema validation, generated read/validate/write, JDK validation of written
XML, and generated re-read/re-validate.

The accepted mapped row set is intentionally small and explicit: the W3C `AttrDecl` fixture
`sunData/AttrDecl/AD_name/AD_name00101m/AD_name00101m1.xsd` plus its positive and negative
instances. The generated-binding execution strips only `xsi:schemaLocation` and
`xsi:noNamespaceSchemaLocation` validator-hint attributes before generated reader input; JDK XML
Schema validation still runs against the original W3C XML. All other W3C rows remain classified as
validation-only, tolerated metadata, expected diagnostics, product-scope-incompatible, or blocked
until explicitly mapped.

No release workflow, version bump, publication behavior, dependency change outside the internal
conformance harness classpath, XSD 1.1/XML 1.1 support, DOM-backed binding, canonical XML behavior,
or `XP-XSD10-FULL` execution is added.

## Verification evidence

- `./gradlew :modules:conformance-tests:test --tests '*W3cXsd10SuiteIntakeTest' --tests '*W3cXsd10SuiteIntakeDeltaTest' --console=plain`
- `./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/tmp/w3c-xsd/extract/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
- W3C summary: `w3c-xsd10-summary total=24796 binding-supported=3 validation-only=24436 tolerated-metadata=98 expected-diagnostic=2 product-scope-incompatible=167 blocked=90`
- Binding execution report: `bindingExecution.passed=1`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
