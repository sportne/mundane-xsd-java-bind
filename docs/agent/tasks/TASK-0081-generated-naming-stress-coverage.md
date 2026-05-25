# TASK-0081: generated-naming-stress-coverage

Status: accepted.

Task ID: `TASK-0081`
Priority: P1
Gate: post-1.0.0 follow-up generated naming coverage.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-018`, `REQ-QA-002`
ADR IDs: `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/naming-collision-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target module: `modules/generator-core`
Allowed files: generator-core generated-source tests and narrow deterministic naming fixes,
conformance fixtures/tests only if needed for an existing supported behavior regression,
verification/conformance docs, traceability docs, and this task/handoff.
Forbidden files: new customization syntax, support expansion beyond existing accepted shapes,
release metadata, dependency metadata, and quality-gate weakening.
Expected behavior: generated Java identifiers remain unique while XML names remain preserved for
`xsi:type`, substitution branches, grouped content branches, and retained wildcard naming stress
cases.
Tests to add/update: targeted generated-source tests that assert both Java identifier uniqueness and
reader/writer metadata or round-trip preservation of XML names for the four stress areas.
Documentation to update: naming-collision review, generated-code contract if naming policy is
clarified, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`, targeted conformance
checks if fixtures change, `./gradlew validateDesignControlPack qualityGate --console=plain`, and
`git diff --check`.
Acceptance criteria: all four stress areas have stable tests; any production fix is narrow and
deterministic; no new customization API or broader schema support is added.
Rollback notes: revert naming tests/fixes/docs.

Completion notes:
- Added generated-source naming stress tests for `xsi:type`, substitution branches, grouped content
  branches, retained wildcard content, and retained wildcard attributes.
- Fixed a generated-reader internal local-name collision by using `XsiTypeName` for the
  `xsi:type` attribute value variable, preserving public generated model field names and XML names.
- Updated naming review and traceability evidence without adding customization syntax or broader
  schema behavior.

Evidence:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
