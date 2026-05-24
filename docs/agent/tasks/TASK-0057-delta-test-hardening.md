# TASK-0057: delta-test-hardening

Status: accepted.

Task ID: `TASK-0057`
Gate: delta test hardening after the last broad hardening cycle.
Baseline commit: `990777c docs: close 0.6.0 readiness review`.
Target areas: post-baseline runtime datatype values, QName XML I/O, `XP-XSD10-FULL` profile token,
full-XSD compiler deltas, W3C suite intake, and related local conformance fixtures
Allowed files: delta unit tests, delta JUnit integration tests, local test fixtures, task/handoff
docs, and production bug fixes limited to a tested post-baseline target class
Forbidden files: new schema support, dependency metadata, release tags, publication behavior,
quality-gate weakening, unrelated pre-baseline hardening work
Expected behavior: harden only production and test surfaces added or changed after `990777c`,
without re-testing already hardened pre-baseline areas except where post-baseline behavior changed.
Tests to add/update: targeted unit tests and limited JUnit integration-style tests for post-baseline
deltas.
Acceptance criteria: every post-`990777c` production Java delta is mapped to direct unit coverage,
public-facade coverage, or documented integration coverage; negative tests assert stable diagnostic
codes/messages where contractual; `XP-XSD10-FULL` remains non-executable and full XSD 1.0 remains
unclaimed.
Rollback notes: revert this task card, handoff changes, and delta hardening tests or narrowly scoped
bug fixes.

## Delta production map

Post-baseline production files are mapped as follows:

```text
runtime-core:
  XmlAnyUri, XmlBinary, XmlDatatypes, XmlDate, XmlDateTime, XmlDuration, XmlGDay,
  XmlGMonth, XmlGMonthDay, XmlGYear, XmlGYearMonth, XmlQName, XmlTime
  -> runtime datatype/value delta tests.
  XmlEventReader, XmlOutput
  -> runtime-jdkxml facade tests for QName namespace lookup/emission.

runtime-jdkxml:
  StaxXmlEventReader -> reader delta tests through JdkXmlAdapters.eventReader.
  StaxXmlOutput -> output delta tests through JdkXmlAdapters.output.

generator-api and generator-cli:
  GeneratorProfile, MxjbCli -> profile-token and planned-full rejection tests.

generator-core:
  SchemaIrBuilder and new/changed SchemaIr* records -> schema IR delta tests and existing IR
  suites.
  BindingModelBuilder and changed Binding* records -> binding delta tests and existing binding
  suites.
  GeneratedModelEmitter, GeneratedReaderEmitter, GeneratedWriterEmitter,
  GeneratedValidatorEmitter -> generated-source and conformance integration tests.
  XmlSchemaBuiltIns -> datatype mapping tests.

conformance-tests:
  W3cXsd10SuiteIntake, W3cXsd10ConformanceMain -> W3C intake delta tests.
  NativeConformanceMain and selected fixture changes -> existing conformance/native compile lanes.
```

## Worker rules

- Workers receive one production target and one mapped test class.
- Workers may edit only their mapped test class plus local fixtures needed by that test.
- Workers may edit the mapped production target only when the new test exposes a real defect in
  that target.
- Cross-class defects must be reported back for orchestration rather than fixed speculatively.

## Completion notes

Accepted on 2026-05-24.

Delta hardening was limited to production and test surfaces added or changed after
`990777c docs: close 0.6.0 readiness review`. Worker-owned tests covered runtime datatype and
QName value behavior, JDK XML QName reader/writer adapter behavior, schema IR metadata and
diagnostics, binding model shape decisions, W3C suite intake classification, and the public
`XP-XSD10-FULL` token/CLI rejection path. Two scoped production defects were exposed and fixed:

- `XmlQName` now rejects lexical QName values whose local part disagrees with the expanded
  `localName`.
- `StaxXmlEventReader` now rejects invalid attribute indexes deterministically instead of allowing
  adapter-specific `NullPointerException` or `null` behavior.

Verification evidence:

```text
./gradlew :modules:runtime-core:check :modules:runtime-jdkxml:check --console=plain
./gradlew :modules:generator-api:check :modules:generator-cli:check --console=plain
./gradlew :modules:generator-core:check --console=plain
./gradlew :modules:conformance-tests:check --console=plain
./gradlew :modules:generator-core:generatedCodeSmoke --console=plain
./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/tmp/w3c-xsd/extract/xmlschema2006-11-06 w3cXsd10Conformance --console=plain
```

`command -v native-image || true` returned no path, so Native Image execution remains locally
blocked by the missing toolchain. No full XSD 1.0 support claim was added, and
`XP-XSD10-FULL` remains non-executable.
