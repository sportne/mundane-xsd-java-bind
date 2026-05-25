# TASK-0069: complexity-architecture-review

Status: draft.

Task ID: `TASK-0069`
Priority: P1
Gate: simplicity and architecture deepening review.
Target areas: generator-core schema IR, binding planner, generated reader/writer/validator emitters,
runtime datatype module, conformance W3C intake module, architecture docs, ADRs, and tests that
expose current module interfaces.
Allowed files: docs, architecture notes, ADR proposals, task cards, and small characterization tests
that make existing behavior safer to refactor.
Forbidden files: broad refactors, feature behavior changes, generated API changes, dependency
changes, release metadata, publication behavior, or quality-gate weakening.
Expected behavior: evaluate whether current module interfaces are deep enough for the behavior they
hide. Apply the deletion test to large modules, especially `SchemaIrBuilder`,
`BindingModelBuilder`, `GeneratedReaderEmitter`, `GeneratedValidatorEmitter`, `GeneratedWriterEmitter`,
`XmlDatatypes`, and `W3cXsd10SuiteIntake`. Produce a prioritized simplification plan with concrete
candidate seams, expected locality/leverage, test strategy, and risks. Do not implement the refactor
in this task unless the change is documentation-only or a small characterization test.
Tests to add/update: optional characterization tests for current behavior that a later refactor must
preserve.
Commands to run: `./gradlew validateDesignControlPack qualityGate --console=plain`,
`git diff --check`; run narrower module checks if characterization tests are added.
Acceptance criteria: the review identifies which complexity is essential, which is accidental, and
which refactors should be tackled first; every proposed refactor has a scoped follow-on task outline
and no ADR conflict is hidden.
Rollback notes: revert architecture notes, characterization tests, and handoff updates.

