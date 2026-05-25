# W3C test-suite policy

W3C XML and XML Schema test suites are reference material for conformance strategy, but passing the full suite is not a phase-one goal.

## Use policy

- Use suite metadata to classify tests by feature/profile.
- Include only tests mapped to current or future profile goals.
- Do not claim full XSD 1.0 conformance until the matrix supports that claim.
- Do not use XSD 1.1 or XML 1.1 fixtures; they are outside the project target.
- Unsupported-feature tests should validate explicit diagnostics.

## Storage policy

Large external test suites should be pulled through documented scripts or Git submodules only after license and maintenance review. Do not vendor large suites in this design-control pack.

`TASK-0017` uses small local `XP-DATA-10` fixtures under `modules/conformance-tests` rather than
vendoring W3C suites. Those fixtures are representative conformance evidence for implemented
features only.

`TASK-0041` keeps `0.6.0` planning in the same posture. `TASK-0042` adds
`modules/conformance-tests/src/test/resources/selected-fixtures.tsv` as selected local fixture
classification evidence. The manifest maps existing local fixtures to declared profiles, adds
minimal local unsupported-diagnostic schemas, and records future-study/blocked rows without
vendoring a broad W3C suite snapshot or claiming full-suite pass status.

`TASK-0046` confirms the `0.6.0` closeout remains selected local evidence only.

`TASK-0055` adds the first pinned W3C XML Schema 1.0 suite intake lane without vendoring the suite.
The accepted reference is the 2007-06-20 archive at
`https://www.w3.org/XML/2004/xml-schema-test-suite/xmlschema2006-11-06/xsts-2007-06-20.tar.gz`,
SHA-256 `902176b25e4111cf96b08663107521a4992e8ea67aad6b815592a6a5b4b9ea06`, extracted as
`xmlschema2006-11-06`. Contributors run it explicitly with
`./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`.
The lane classifies W3C `.testSet` metadata as binding-supported, validation-only,
tolerated-metadata, expected-diagnostic, product-scope-incompatible, or blocked. It writes local
reports under `build/` and remains outside `qualityGate`.

`TASK-0056` reconciles that intake as evidence only. `TASK-0064` adds the first explicit
generated-binding mapping for three W3C `AttrDecl` rows, `TASK-0070` adds one W3C wildcard
schema plus its positive and negative instances, and `TASK-0080` adds a strict `anyAttribute`
wildcard schema plus its positive and negative instances. The current mapped executions generate,
compile, read, validate, write, re-read, and re-validate the bindings while preserving JDK XML
Schema validation as the oracle. The lane strips only `xsi:schemaLocation` and
`xsi:noNamespaceSchemaLocation` validator hints before generated reader input. The project does not
claim a W3C full-suite generated-binding pass beyond explicitly mapped rows; unmapped rows remain
classified as validation-only, tolerated metadata, expected diagnostics, product-scope-incompatible,
or blocked.

`TASK-0083` keeps that report and execution behavior unchanged while separating the generated
binding execution mechanics into `W3cXsd10BindingExecutor`. `W3cXsd10SuiteIntake` remains
responsible for suite-root validation, metadata parsing, classification, and report writing.
