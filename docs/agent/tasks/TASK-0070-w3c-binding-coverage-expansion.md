# TASK-0070: w3c-binding-coverage-expansion

Status: accepted.

Task ID: `TASK-0070`
Priority: P1
Gate: W3C generated-binding coverage expansion.
Target areas: W3C suite intake classification, generated-binding mapping rules, conformance tests,
selected local fixtures, conformance matrix, verification plan, and task handoff.
Allowed files: conformance harness code, W3C mapping metadata, small local fixtures, docs, and tests.
Forbidden files: product schema behavior expansion unless separately authorized, broad W3C suite
vendoring, dependencies, release metadata, XSD 1.1/XML 1.1, Maven Central publishing, signing, or
quality-gate weakening.
Expected behavior: expand W3C XML Schema 1.0 generated-binding execution beyond the initial three
`AttrDecl` rows. Prioritize rows that exercise content models, wildcards, derivation/polymorphism,
identity constraints, datatype facets, and include/import behavior already claimed by the project.
Rows outside generated-binding product scope must remain classified with documented rationale.
Tests to add/update: W3C mapping tests, generated-binding execution tests, semantic comparison tests,
and deterministic diagnostics for rows that remain expected diagnostics.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
when the local suite is available, `./gradlew validateDesignControlPack qualityGate --console=plain`,
and `git diff --check`.
Acceptance criteria: mapped binding-supported row count increases with passing generate/compile/read/
validate/write/re-read evidence; unmapped rows remain honest classifications; no broad full-suite
pass is claimed unless every in-scope row is actually executed.
Rollback notes: revert mapping metadata, fixture/test additions, and docs.

## Completion notes

`TASK-0070` expands W3C XML Schema 1.0 generated-binding evidence without vendoring the W3C suite
or changing product schema behavior. The accepted expansion adds the W3C wildcard fixture
`sunData/Wildcard/nsConstraint/nsConstraint00101m/nsConstraint00101m1.xsd` plus its positive and
negative instances to the explicit generated-binding mapping set. The row was selected because it
uses inline `xs:any` wildcard content that is already supported by the generator; model-group and
`anyAttribute` candidates that exposed unprefixed QName reference limitations were left unmapped
instead of expanding behavior in this task.

The pinned local suite evidence now reports:

```text
w3c-xsd10-summary total=24796 binding-supported=6 validation-only=24433 tolerated-metadata=98 expected-diagnostic=2 product-scope-incompatible=167 blocked=90
w3c-xsd10-binding-execution passed=2
```

The six binding-supported rows are the three accepted `TASK-0064` `AttrDecl` rows plus the three
accepted `TASK-0070` wildcard schema/instance rows. Remaining W3C rows are still classification
evidence only.

## Evidence

- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/w3c/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
