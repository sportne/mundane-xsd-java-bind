# TASK-0047: archunit-architecture-rule-hardening

Status: accepted.

Task ID: `TASK-0047`
Gate: Ad hoc hardening gate before `TASK-0019` promotion.
Requirement IDs: `REQ-RT-001`, `REQ-RT-002`, `REQ-GEN-002`, `REQ-QA-001`, `REQ-NI-001`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/architecture-rule-catalog.md`, `docs/architecture/native-image-architecture.md`, `docs/architecture/generated-code-contract.md`
Target modules: `modules/runtime-core`, `modules/runtime-jdkxml`, `modules/generator-api`, `modules/generator-core`, `modules/generator-cli`
Allowed files: architecture docs, task/handoff docs, ArchUnit tests, generated-source emitter tests
Forbidden files: Gradle plugin behavior, production behavior changes, dependency metadata, runtime reflection/configuration workarounds
Expected behavior: document and enforce architecture rules in project-specific, Native Image, and general Java baseline categories.
Tests to add/update: module ArchUnit tests and generated-source forbidden-token tests.
Documentation to update: architecture rule catalog and handoff.
Commands to run: `./gradlew :modules:runtime-core:check :modules:runtime-jdkxml:check :modules:generator-api:check :modules:generator-core:check :modules:generator-cli:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: rules are documented, production main code passes new ArchUnit checks, generated source remains free of forbidden mechanisms, and test-only reflection/classloader harness usage remains outside enforced scopes.
Rollback notes: revert this task card, rule catalog, handoff updates, and related architecture/source-test changes.

## Completion Notes

- Added the architecture rule catalog with project-specific, Native Image, and general Java baseline rule categories.
- Expanded runtime, generator API, generator core, and CLI architecture tests for forbidden dependencies, dynamic mechanisms, serialization, internal JDK APIs, process termination/spawning, finalizers, and mutable public static state.
- Expanded generated-source emitter tests to reject additional Native Image-hostile and baseline-forbidden tokens.
- Verification: `./gradlew :modules:runtime-core:check :modules:runtime-jdkxml:check :modules:generator-api:check :modules:generator-core:check :modules:generator-cli:check`, `./gradlew validateDesignControlPack qualityGate`, and `git diff --check` passed before acceptance was recorded.
