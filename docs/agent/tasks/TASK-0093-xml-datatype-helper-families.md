# TASK-0093: xml-datatype-helper-families

Status: accepted.

Task ID: `TASK-0093`
Priority: P2
Gate: deeper runtime architecture refactor.
Requirement IDs: `REQ-RT-001`, `REQ-VAL-003`, `REQ-VAL-005`, `REQ-QA-002`
ADR IDs: `ADR-0003`, `ADR-0005`, `ADR-0007`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/runtime-architecture.md`, `docs/architecture/validation-architecture.md`,
`docs/requirements/traceability-matrix.md`
Target module: `modules/runtime-core`
Allowed files: runtime-core package-private datatype helper classes and tests, architecture docs,
traceability docs, and this task/handoff.
Forbidden files: public runtime API changes, generated source behavior changes, dependency metadata,
release metadata, schema support expansion, and quality-gate weakening.
Expected behavior: group `XmlDatatypes` internals behind smaller lexical, numeric/range,
date-time, QName, binary, and list helper families while preserving the public `XmlDatatypes` API
and all lexical/facet behavior.
Tests to add/update: focused helper tests where useful plus existing runtime datatype and generated
datatype behavior locks.
Documentation to update: complexity review, runtime/validation architecture if vocabulary changes,
traceability, and handoff.
Commands to run: `./gradlew :modules:runtime-core:check --console=plain`, plus
`./gradlew :modules:generator-core:check --console=plain` if generated datatype paths are touched,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: datatype helper families are package-private; `XmlDatatypes` public behavior
is unchanged; no dependency or generated source behavior changes are introduced.
Rollback notes: revert helper extraction, tests, and docs.

Completion notes:
- Added package-private `XmlDatatypeLexical`, `XmlDatatypeNumeric`, `XmlDatatypeDateTime`,
  `XmlDatatypeQNames`, `XmlDatatypeBinary`, and `XmlDatatypeLists` helper families.
- Kept `XmlDatatypes` as the public generated-code-facing facade and retained package-private
  bridge methods used by existing datatype value classes.
- No dependency, public runtime API, generated source, or supported-datatype expansion was
  introduced.
- Subagent review identified a pre-existing `base64Binary` lexical hole in the moved code; the
  task now strips XML whitespace before strict Base64 decoding and rejects non-base64 alphabet
  characters.

Evidence:
- `./gradlew :modules:runtime-core:compileJava --console=plain`
- `./gradlew :modules:runtime-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
- Subagent review: one no-findings review and one base64 lexical-validation finding; finding fixed
  before commit.
