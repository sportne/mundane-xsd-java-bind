# TASK-0036: document-oriented-open-content-planning

Status: draft.

Task ID: `TASK-0036`
Gate: `0.5.0` Document-Oriented and Open Content planning; starts only after `TASK-0035` is accepted.
Requirement IDs: future `REQ-SCHEMA-*`, future `REQ-BIND-*`, future `REQ-XML-W-*`, future `REQ-XML-R-*`, future `REQ-VAL-*`, `REQ-GEN-*`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`, `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define accepted wildcard, open-content, mixed-content, unknown XML retention, and canonicalization/serialization policy for `0.5.0`, including generated model shapes, interop fixtures, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0037` through `TASK-0040`
Documentation to update: compatibility profiles, generated-code contract, runtime architecture, conformance matrix, traceability matrix, follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: open-content semantics are decision-complete before implementation; interop candidates are identified; no DOM-first runtime is introduced without ADR
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose document-like fixtures with external validation or serialization comparison where practical.
- Native Image: define open-content fixtures for selected lanes.
- Security: unknown content handling must preserve resolver and parser safety.
- Documentation: avoid implying arbitrary DOM-based binding.
