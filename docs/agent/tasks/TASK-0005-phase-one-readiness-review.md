# TASK-0005: phase-one-readiness-review

Status: pending.

Requirement IDs: `REQ-SCHEMA-*`, `REQ-GEN-*`, `REQ-XML-*`, `REQ-VAL-*`
ADR IDs: `ADR-0001` through `ADR-0014`
Allowed files: requirements, architecture, ADRs, conformance matrix, agent task cards
Forbidden files: product implementation source unless a new approved task card is created
Expected behavior: produce approved implementation task cards for the first schema compiler vertical slice.
Tests to add/update: planned tests only.
Commands to run: `./gradlew validateDesignControlPack qualityGate`
Acceptance criteria: first implementation task has requirements, allowed files, test plan, documentation plan, and acceptance criteria.
Rollback notes: revert readiness-review docs.
