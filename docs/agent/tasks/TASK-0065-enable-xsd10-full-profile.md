# TASK-0065: enable-xsd10-full-profile

Status: draft.

Task ID: `TASK-0065`
Gate: enable executable `XP-XSD10-FULL`.
Target areas: generator API/core/CLI/Gradle plugin profile handling, docs, generated-code smoke,
conformance tests, and compatibility profile declarations.
Allowed files: generator API/core/CLI/Gradle plugin production/tests, conformance fixtures/tests,
generated-code smoke fixtures, README/module docs, compatibility/conformance/verification docs,
and task evidence.
Forbidden files: release workflow, release tag, publication behavior, Maven Central, signing,
dependencies, XSD 1.1, XML 1.1, canonical XML, DOM-backed binding, or version bump.
Expected behavior: make `GeneratorProfile.XP_XSD10_FULL` executable only after `TASK-0059` through
`TASK-0064` evidence passes; remove planned-token rejection paths and update public docs to describe
the full profile with explicit non-goals.
Tests to add/update: API/CLI/Gradle profile acceptance tests; CoreGenerator success and diagnostic
tests; generated-code smoke for representative full-profile schemas; W3C mapped-row conformance
checks.
Commands to run: `./gradlew :modules:generator-api:check :modules:generator-cli:check
:modules:generator-gradle-plugin:check :modules:generator-core:check :modules:conformance-tests:check
--console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`.
Acceptance criteria: `XP-XSD10-FULL` generation succeeds through API, CLI, Gradle plugin, and
CoreGenerator; unsupported non-goals still produce deterministic diagnostics; docs no longer call
the profile planned/non-executable.
Rollback notes: restore non-executable profile rejection paths, docs, fixtures, and task status.
