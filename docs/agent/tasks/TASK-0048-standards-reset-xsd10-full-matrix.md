# TASK-0048: standards-reset-xsd10-full-matrix

Status: accepted.

Task ID: `TASK-0048`
Gate: `XP-XSD10-FULL` program planning; starts after `TASK-0046`.
Requirement IDs: `REQ-SCHEMA-014`, `REQ-CONF-002`, `REQ-AGENT-001`
ADR IDs: `ADR-0001`, `ADR-0006`
Specification references: `docs/verification/xsd10-full-feature-matrix.md`, `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`
Target areas: public profile token, standards scope docs, conformance manifest, task sequence, handoff
Allowed files: generator API profile enum/tests, CLI help/tests, conformance manifest/tests, Native Image selected unsupported list, docs, task cards
Forbidden files: product support for new XSD constructs, XSD 1.1/XML 1.1 future profiles, release tags, publication, dependency changes
Expected behavior: define `XP-XSD10-FULL` as the only full-standard target while keeping it non-executable until follow-on tasks implement it.
Tests to add/update: profile parsing tests, CLI planned-token diagnostic test, selected fixture manifest tests, native conformance compile path
Documentation to update: standards baseline, compatibility profiles, conformance matrix, roadmap, README, task handoff, traceability, W3C policy
Commands to run: `./gradlew :modules:generator-api:check :modules:generator-cli:check :modules:conformance-tests:compileNativeConformanceJava :modules:conformance-tests:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: active docs no longer describe XSD 1.1 or XML 1.1 as future goals; `XP-XSD10-FULL` has a feature matrix and follow-on task sequence; executable profiles remain unchanged.
Rollback notes: remove the `XP-XSD10-FULL` planning token, matrix, and task-card sequence; restore the previous conformance manifest.

## Completion Notes

- Added the public `XP-XSD10-FULL` profile token as a planned target while keeping `CoreGenerator`
  deterministic rejection for that profile.
- Removed XSD 1.1/XML 1.1 future-profile wiring from active compatibility, standards, conformance,
  manifest, and handoff docs.
- Added the XSD 1.0 full feature matrix and opened draft task cards for `TASK-0049` through
  `TASK-0056`.
- Kept current executable behavior limited to the accepted profiles through `XP-XSD10-DOCUMENT`.

## Verification Evidence

- `./gradlew :modules:generator-api:check :modules:generator-cli:check :modules:generator-core:check :modules:conformance-tests:compileNativeConformanceJava :modules:conformance-tests:check --console=plain`
  passed after one Spotless formatting correction.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
