# TASK-0003: quality-gate-wiring

Status: completed for the current scaffold; repeat when quality tooling or CI gate behavior changes.

Requirement IDs: `REQ-QA-001`, `REQ-BUILD-001`
ADR IDs: `ADR-0011`
Allowed files: build scripts, config quality files, CI workflows
Forbidden files: XML implementation source
Expected behavior: Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, and JaCoCo are wired without source-code implementation.
Tests to add/update: build logic tests if added.
Commands to run: `./gradlew check`, `./gradlew qualityGate`
Acceptance criteria: `check` and `qualityGate` succeed.
Rollback notes: revert build logic changes.
