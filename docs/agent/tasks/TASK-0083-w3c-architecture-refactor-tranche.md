# TASK-0083: w3c-architecture-refactor-tranche

Status: accepted.

Task ID: `TASK-0083`
Priority: P2
Gate: post-1.0.0 follow-up architecture refactor.
Requirement IDs: `REQ-CONF-002`, `REQ-QA-002`
ADR IDs: `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/conformance/w3c-test-suite-policy.md`, `docs/verification/conformance-strategy.md`
Target module: `modules/conformance-tests`
Allowed files: W3C conformance intake/execution classes and tests, architecture/conformance docs,
traceability docs, and this task/handoff.
Forbidden files: IR normalization, generated emitter planning, datatype helper refactors, binding
planner refactors, product behavior expansion, dependency metadata, release metadata, and
quality-gate weakening.
Expected behavior: separate W3C suite intake/parsing/classification from generated-binding
execution inside the conformance module. Preserve existing command-line behavior, report format,
counts, and generated-binding execution semantics.
Tests to add/update: characterization tests that prove parse/classification reports and
generated-binding execution remain equivalent after separation; W3C opt-in lane when local suite is
available.
Documentation to update: complexity review, conformance strategy or W3C policy if module shape is
clarified, traceability, and handoff.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew -Pmxjb.w3cXsd10SuiteDir=build/w3c/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
when the local suite exists, `./gradlew validateDesignControlPack qualityGate --console=plain`, and
`git diff --check`.
Acceptance criteria: W3C intake/classification and generated-binding execution responsibilities are
separated without behavior or report drift; future refactor candidates remain documented rather than
implemented in this task.
Rollback notes: revert W3C refactor and related docs/tests.

Completion notes:
- Extracted explicitly mapped generated-binding execution into package-private
  `W3cXsd10BindingExecutor`.
- Kept suite-root validation, metadata parsing, classification, expected diagnostic execution, and
  report writing in `W3cXsd10SuiteIntake`.
- Preserved mapped-row validation, generation, compilation, read/validate/write/re-read semantics,
  command-line summary output, report filenames, and W3C evidence counts.
- Updated architecture, conformance, security, traceability, and handoff docs while leaving larger
  IR, emitter, datatype, and binding-planner refactors as future candidates.

Evidence:
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/w3c/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
