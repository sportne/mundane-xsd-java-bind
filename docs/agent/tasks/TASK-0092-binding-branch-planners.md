# TASK-0092: binding-branch-planners

Status: accepted.

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

## Completion Notes

`BindingSubstitutionPlanner` now owns direct substitution-group choice branch assembly and
substitution branch diagnostics. `BindingDynamicTypePlanner` now owns declared-base dynamic
`xsi:type` branch discovery, block-control filtering, default-branch planning, and dynamic branch
diagnostics. `BindingModelBuilder` retains schema indexing, type-reference binding, field assembly
coordination, and sorted diagnostic ownership.

Focused planner tests cover substitution branch order and unsupported branch-type diagnostics,
dynamic `xsi:type` default/candidate ordering, and blocked dynamic-candidate suppression.

## Verification

- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
- Subagent review of the TASK-0092 diff and new files reported no findings.
