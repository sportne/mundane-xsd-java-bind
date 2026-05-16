# TASK-0026: composed-xsd10-planning

Status: draft.

Task ID: `TASK-0026`
Gate: `0.3.0` Composed XSD 1.0 Schemas planning; starts only after `TASK-0025` is accepted.
Requirement IDs: future `REQ-SCHEMA-*`, future `REQ-BIND-*`, future `REQ-VAL-*`, `REQ-GEN-*`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/compiler-pipeline.md`, `docs/verification/conformance-strategy.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the `0.3.0` composed-schema scope for named model groups, attribute groups, simple type list/union, and initial derivation; add requirement IDs, conformance rows, interop fixture candidates, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0027` through `TASK-0030`
Documentation to update: compatibility profiles, conformance matrix, architecture docs, traceability matrix, and follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: `0.3.0` support is decision-complete and still narrower than full XSD 1.0 unless explicitly accepted; interop candidates are identified for each feature group
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose modular-schema fixtures that can be validated by external XML Schema tooling where practical.
- Native Image: define which composed-schema fixtures enter native lanes.
- Security: include composition depth and cycle considerations.
- Documentation: no full-XSD conformance claims.
