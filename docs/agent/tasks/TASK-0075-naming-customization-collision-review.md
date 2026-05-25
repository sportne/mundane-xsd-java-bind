# TASK-0075: naming-customization-collision-review

Status: draft.

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

