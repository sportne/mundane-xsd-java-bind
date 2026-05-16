# TASK-0004: staged-gate-hardening

Status: completed for the scaffold and implemented generator-core source; empty modules retain explicit staged skips until they contain compiled production classes.

Requirement IDs: `REQ-QA-001`, `REQ-NI-001`
ADR IDs: `ADR-0010`, `ADR-0011`
Allowed files: quality config, coverage policy, native-image workflow, CI docs
Forbidden files: XML implementation source
Expected behavior: staged gates are documented with explicit dates/phases for becoming hard failures.
Tests to add/update: CI dry-run or validation script.
Commands to run: `./gradlew validateDesignControlPack`, `./gradlew qualityGate`
Acceptance criteria: quality gates are not silently skipped and staged exceptions are documented.
Rollback notes: revert gate config changes.
