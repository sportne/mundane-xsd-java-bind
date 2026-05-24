# TASK-0055: xsd10-full-conformance-suite

Status: accepted.

Task ID: `TASK-0055`
Gate: pinned XSD 1.0 conformance suite intake.
Target areas: conformance-tests, fixture manifests, W3C suite policy, verification docs, CI docs
Allowed files: conformance test harness, local selected fixtures or pinned metadata, docs
Forbidden files: networked test retrieval in normal gates, broad vendoring without storage/license review, product behavior changes, XSD 1.1/XML 1.1
Expected behavior: add a repeatable W3C XML Schema 1.0 suite intake process with fixture classification for binding-supported, validation-only, tolerated metadata, expected diagnostic, and incompatible product-scope cases.
Tests to add/update: manifest parser tests, suite classification tests, generated-binding/JDK comparison tests for accepted fixtures.
Acceptance criteria: the conformance suite can be run locally from pinned inputs and produces stable summarized evidence without claiming unsupported scope.
Rollback notes: remove suite intake wiring and generated classifications from this task.

## Completion notes

- Added the opt-in `w3cXsd10Conformance` lane at the root and under
  `:modules:conformance-tests`. It is separate from `check`, `checkAll`, `qualityGate`,
  `benchmarkSmoke`, `nativeSmoke`, and `nativeConformance`.
- Pinned the W3C XML Schema 1.0 suite release to
  `https://www.w3.org/XML/2004/xml-schema-test-suite/xmlschema2006-11-06/xsts-2007-06-20.tar.gz`
  with archive SHA-256
  `902176b25e4111cf96b08663107521a4992e8ea67aad6b815592a6a5b4b9ea06` and expected extracted root
  `xmlschema2006-11-06`.
- Implemented dependency-free `.testSet` metadata parsing, secure JDK XML parser configuration,
  duplicate-ID checks, expected-validity/status checks, referenced-file checks, XSD 1.1/XML 1.1
  rejection, deterministic classification, and `build/reports/w3c-xsd10-conformance` report
  output.
- Full local W3C suite evidence from the pinned archive:
  `w3c-xsd10-summary total=24796 binding-supported=0 validation-only=24439 tolerated-metadata=98 expected-diagnostic=2 product-scope-incompatible=167 blocked=90`.
- The two `expected-diagnostic` rows are W3C redefine schema fixtures executed through
  `CoreGenerator` to prove deterministic unsuccessful generation. Other rows are classification
  evidence until they are explicitly mapped to generated-binding support.

## Verification evidence

- `./gradlew :modules:conformance-tests:test --tests '*W3cXsd10SuiteIntakeTest*' --console=plain`
  passed.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
- `./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/tmp/w3c-xsd/extract/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
  passed with the summary above.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
