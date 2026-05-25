# TASK-0092: binding-branch-planners

Status: draft.

Task ID: `TASK-0092`
Priority: P2
Gate: deeper generator architecture refactor.
Requirement IDs: `REQ-BIND-001`, `REQ-GEN-001`, `REQ-SCHEMA-018`, `REQ-QA-002`
ADR IDs: `ADR-0006`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/verification/naming-collision-review.md`,
`docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: binding package-private branch planners and tests, architecture/naming docs,
traceability docs, and this task/handoff.
Forbidden files: public binding model API changes, generated source behavior changes, schema support
expansion, dependency metadata, release metadata, and quality-gate weakening.
Expected behavior: split substitution group and declared-base `xsi:type` dynamic branch planning out
of `BindingModelBuilder` while preserving `BindingModel` shape, ordering, diagnostics, Java names,
and XML names.
Tests to add/update: focused branch-planner tests plus existing binding/generator naming stress
coverage.
Documentation to update: complexity review, naming collision review if ownership changes,
traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: substitution and `xsi:type` branch planning have explicit package-private
helper ownership; generated source behavior remains unchanged.
Rollback notes: revert branch planner extraction, tests, and docs.
