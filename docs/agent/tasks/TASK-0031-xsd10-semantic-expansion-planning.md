# TASK-0031: xsd10-semantic-expansion-planning

Status: draft.

Task ID: `TASK-0031`
Gate: `0.4.0` XSD 1.0 Semantic Expansion planning; starts only after `TASK-0030` is accepted.
Requirement IDs: future `REQ-SCHEMA-*`, future `REQ-BIND-*`, future `REQ-VAL-*`, `REQ-GEN-*`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/validation-architecture.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the `0.4.0` semantic scope for `nillable`, `default`, `fixed`, substitution groups, and broader validation semantics; record requirements, model-shape decisions, interop fixture candidates, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0032` through `TASK-0035`
Documentation to update: compatibility profiles, generated-code contract, validation architecture, conformance matrix, traceability matrix, follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: semantic behavior is decision-complete before implementation; any generated model shape changes are documented; interop candidates are identified
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose semantic fixtures where external validators can verify value/default/nillable outcomes.
- Native Image: define representative semantic fixtures for native lanes.
- Security: validation diagnostics must remain path-safe.
- Documentation: do not mix document-oriented open content into this slice.
