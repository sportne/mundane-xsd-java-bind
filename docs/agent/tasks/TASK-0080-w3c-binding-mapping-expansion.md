# TASK-0080: w3c-binding-mapping-expansion

Status: accepted.

Task ID: `TASK-0080`
Priority: P1
Gate: post-1.0.0 follow-up W3C evidence.
Requirement IDs: `REQ-CONF-002`, `REQ-SCHEMA-019`, `REQ-QA-002`
ADR IDs: `ADR-0001`, `ADR-0006`, `ADR-0011`, `ADR-0013`
Specification references: `docs/conformance/w3c-test-suite-policy.md`,
`docs/verification/conformance-strategy.md`, `docs/verification/xsd10-full-feature-matrix.md`
Target module: `modules/conformance-tests`
Allowed files: W3C intake/mapping code and tests, conformance test fixtures needed for tiny local
synthetic W3C metadata tests, conformance/verification docs, traceability docs, and this
task/handoff.
Forbidden files: vendored W3C suite files, product behavior expansion, release metadata, dependency
metadata, broad W3C full-suite support claims, and quality-gate weakening.
Expected behavior: add the next small batch of explicitly mapped W3C generated-binding rows from
already supported local behavior. Prioritize choice/content-model, datatype, identity, or derivation
rows that can execute generate, compile, read, validate, write, and re-read deterministically.
Tests to add/update: W3C intake tests for the added mapped rows and generated-binding execution
assertions; docs/tests must preserve unmapped rows as classification evidence only.
Documentation to update: conformance strategy, W3C suite policy, conformance matrix, XSD 1.0 feature
matrix, traceability, and handoff.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=build/w3c/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: newly mapped rows are counted as binding-supported only when executable
generated-binding evidence passes; summary counts and docs match the implementation; no W3C suite
content is tracked.
Rollback notes: revert W3C mapping/test/doc updates.

Completion notes:
- Added an explicit W3C generated-binding mapping for
  `sunData/Wildcard/psContents/psContents00102m/psContents00102m1.xsd` plus its positive and
  negative instances.
- Preserved the existing `AttrDecl` and `nsConstraint` mappings and kept all unmapped W3C rows as
  classification evidence only.
- Updated conformance/verification/traceability docs to report nine binding-supported rows and
  three generated-binding executions without broad full-suite claims.

Evidence:
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/w3c/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
