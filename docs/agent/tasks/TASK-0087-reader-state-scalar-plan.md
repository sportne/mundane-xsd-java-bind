# TASK-0087: reader-state-scalar-plan

Status: draft.

Task ID: `TASK-0087`
Priority: P2
Gate: deeper generator architecture refactor.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-QA-002`
ADR IDs: `ADR-0004`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: reader emitter package-private plan/helper classes and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public API changes, runtime dependency changes, broad reader traversal rewrites,
generated source behavior changes, release metadata, dependency metadata, and quality-gate
weakening.
Expected behavior: add explicit package-private reader state-machine and scalar-conversion plan
objects before reader source text assembly while preserving generated Java source behavior.
Tests to add/update: focused plan characterization tests plus existing generated-source
compile/smoke coverage.
Documentation to update: complexity review, generated-code contract if plan vocabulary changes,
traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: reader source-state/scalar decisions are represented by internal plan objects;
reader output behavior is unchanged; no public API or runtime dependency changes are introduced.
Rollback notes: revert reader plan helpers, tests, and docs.
