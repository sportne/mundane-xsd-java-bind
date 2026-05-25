# TASK-0075: naming-customization-collision-review

Status: accepted.

Task ID: `TASK-0075`
Priority: P2
Gate: naming, customization, and collision review.
Target areas: namespace-to-package mapping, Java type/member naming, content branch naming,
substitution/dynamic branch naming, Gradle/CLI customization, docs, and generated-source tests.
Allowed files: generator-core tests, selected fixtures, docs, and narrow deterministic naming fixes.
Forbidden files: new customization language, dependencies, release metadata, schema-feature
expansion, or quality-gate weakening unless a later ADR authorizes the scope.
Expected behavior: stress generated names with duplicate local names across namespaces, Java
keywords, nested anonymous types, repeated groups, substitution members, `xsi:type` branches,
wildcard/content branch names, enum-like values, and user package mappings. Compare against
ecosystem issues around duplicate classes and confusing customizations.
Tests to add/update: generated-source determinism, collision-safe names, CLI/Gradle package mapping
failures, and docs examples.
Commands to run: `./gradlew :modules:generator-core:check :modules:generator-cli:check :modules:generator-gradle-plugin:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: known collision classes are covered by tests; generated names are deterministic
and explainable; unsupported customization requests fail with useful diagnostics.
Rollback notes: revert naming tests/docs/fixes.

## Completion notes

`TASK-0075` adds `docs/verification/naming-collision-review.md` and generated-source regression
coverage for two concrete collision classes: duplicate schema local type names mapped into one Java
package, and Java keyword element/attribute names. The duplicate-local-name case proves deterministic
suffixing (`Order`, `Order2`, and matching helper names) and compiles the generated sources. The
keyword case proves generated Java fields are escaped while XML names remain represented by the
generated reader/writer metadata.

The existing duplicate-root-helper collision test remains the unsupported collision evidence: two
root elements sharing one model type still fail before writing partial output. Invalid package
customization keeps the existing diagnostic code and now gives Java package syntax guidance.

No new customization language, dependency, release metadata, schema-feature expansion, or
quality-gate weakening is introduced.

## Evidence

- `./gradlew :modules:generator-core:check :modules:generator-cli:check :modules:generator-gradle-plugin:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
