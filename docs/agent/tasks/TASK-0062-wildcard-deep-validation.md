# TASK-0062: wildcard-deep-validation

Status: draft.

Task ID: `TASK-0062`
Gate: complete wildcard strict/lax validation and derivation composition.
Target areas: wildcard namespace algebra, global declaration lookup, fragment-backed validation,
binding metadata, generated reader/validator helpers, generator-core tests, and conformance
fixtures.
Allowed files: generator-core production/tests, conformance fixtures/tests, generated-code smoke
fixtures, and docs for validation/security updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, lexical prefix preservation, DOM-backed binding, or enabling
`XP-XSD10-FULL`.
Expected behavior: complete `processContents="strict"` and `"lax"` schema-known validation for
retained element and attribute wildcards, wildcard choice branches, and wildcard derivation
composition while preserving existing retained-fragment policy.
Tests to add/update: unit tests for wildcard union/intersection/restriction; generated
reader/validator tests for strict/lax known and unknown declarations; selected JDK XML Schema
comparison fixtures for element and attribute wildcard cases.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: strict wildcards reject unknown matching names, lax wildcards validate known
declarations and retain unknown names, wildcard derivation composition matches XSD 1.0 rules for
supported generated shapes, and no lexical-prefix or DOM-backed claims are added.
Rollback notes: revert wildcard validation/composition changes, fixtures, docs, and task status.
