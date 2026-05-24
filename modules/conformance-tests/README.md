# conformance-tests

Internal conformance test harness; not published.

## Current status

Selected local `XP-DATA-10` fixtures exercise the first public vertical slice without claiming full
W3C XML Schema conformance. The tests reuse the checked-in generated fixture sources from the
purchase-order and multi-namespace examples.

`src/test/resources/selected-fixtures.tsv` is the selected fixture manifest for the `0.6.0`
hardening lane. It classifies local evidence as `supported-profile`, `unsupported-diagnostic`,
`future-study`, or `blocked`, and maps executable rows to either existing profile fixtures or
minimal unsupported schemas. `SelectedConformanceFixtureManifestTest` verifies the manifest shape,
resource presence, supported-profile coverage, and deterministic generator diagnostic codes for the
unsupported rows.

`nativeConformance` is the opt-in GraalVM Native Image conformance lane added for `TASK-0044`.
It builds selected generated bindings during Gradle setup, compiles a static native executable, and
runs supported-profile round trips, selected unsupported diagnostics, and secure entity/resource
denial. It is separate from `check`, `qualityGate`, and the representative `nativeSmoke` aggregate.

`TASK-0046` treats this module as selected local readiness evidence for the `0.6.0` hardening
slice. It is not broad W3C suite coverage and does not change supported schema behavior.

`w3cXsd10Conformance` is the opt-in W3C XML Schema 1.0 suite intake lane added for `TASK-0055`.
It requires a local extracted `xmlschema2006-11-06` directory from the pinned 2007-06-20 W3C
archive and is run explicitly:

```bash
./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain
```

The lane parses `.testSet` metadata, validates referenced files, rejects XSD 1.1/XML 1.1 intake,
executes expected generator diagnostics where classified, and writes `fixtures.tsv` plus
`summary.txt` under `build/reports/w3c-xsd10-conformance`. It is classification evidence only; the
W3C suite archive and extracted files are not checked in.

`TASK-0056` reconciles the suite intake with the feature matrix. The current W3C summary has zero
generated-binding-supported rows, so this module still does not prove full XML Schema 1.0
conformance or make `XP-XSD10-FULL` executable.

Covered behavior:

- XML to object to XML round trips for representative generated bindings.
- Namespace-aware element matching across imported-schema fixture boundaries.
- Required-content, out-of-order sequence, namespace mismatch, and scalar lexical diagnostics.
- Opt-in `xs:choice`, `XP-VALIDATION-10-BASIC` facet, `XP-XSD10-COMPOSED` composed-schema and
  datatype/facet, `XP-XSD10-SEMANTIC` semantic and identity-constraint, and accepted
  `XP-XSD10-DOCUMENT` wildcard, mixed-content, strict/lax schema-known wildcard validation, and
  serialization-policy fixtures compared with JDK XML Schema validation where practical.
- Selected unsupported-diagnostic schemas for constructs that still sit outside the accepted
  generated-binding shapes.

Unsupported or future-profile constructs such as remaining derivation/restriction/block/final and
`xsi:type` edges, W3C generated-binding row mapping, identity-constraint edge cases beyond accepted
generated model shapes, and full XSD 1.0 are outside this harness until
their task cards are accepted.

## Contributor notes

- Keep conformance scope aligned with `docs/conformance/`.
- Tag slow, integration, and native-image tests explicitly.
- Do not add network access except in explicitly tagged integration tests.
- Do not vendor broad external W3C suites without the policy review described in
  `docs/conformance/w3c-test-suite-policy.md`.
