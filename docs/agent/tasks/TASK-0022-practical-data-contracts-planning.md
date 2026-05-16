# TASK-0022: practical-data-contracts-planning

Status: draft.

Task ID: `TASK-0022`
Gate: `0.2.0` Practical Data Contracts planning; starts only after `TASK-0021` is accepted.
Requirement IDs: `REQ-SCHEMA-007`, `REQ-VAL-003`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/conformance-strategy.md`, `docs/verification/verification-plan.md`
Target areas: requirements, conformance matrix, verification docs, and draft task cards
Allowed files: requirements docs, conformance docs, verification docs, architecture docs, ADRs if scope changes require them, and agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the exact `XP-DATA-10-CHOICE` and `XP-VALIDATION-10-BASIC` support for `0.2.0`, including accepted choice shapes, accepted simple restriction facets, test IDs, interop fixture candidates, unsupported diagnostics, and readiness criteria.
Tests to add/update: planned tests only; record expected golden, round-trip, negative, Native Image, and interop evidence for `TASK-0023` through `TASK-0025`
Documentation to update: compatibility profiles, conformance matrix, verification plan, traceability matrix, and follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: `0.2.0` has decision-complete task cards; no implementation is authorized by this task; interop expectations are listed for each accepted feature where a JDK/XML reference can be useful
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose concrete `0.2.0` fixtures for comparison against JDK XML validation or other approved references where practical.
- Native Image: define whether new choice/facet fixtures join smoke or conformance lanes.
- Security: preserve existing resolver and reader denial policies.
- Documentation: avoid claiming `XP-XSD10-FULL`.
