# TASK-0012: generated-writer-emitter

Status: draft.

Task ID: `TASK-0012`
Gate: Phase 3 generated model and writer vertical slice; starts only after `TASK-0011` is accepted.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-W-001`, `REQ-NS-001`, `REQ-RT-001`, `REQ-RT-002`, `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0005`, `ADR-0006`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/architecture/compiler-pipeline.md`
Target module: `modules/generator-core`
Allowed files: generator-core writer emitter source/tests/resources/golden writer fixtures, generated compile-test fixtures, and directly related docs
Forbidden files: runtime module product source, public `generator-api` source, CLI source, Gradle plugin source, XML reader implementation, validation engine implementation, dependency metadata, and committed generated product code outside approved golden fixtures
Expected behavior: emit deterministic XML writer source that writes supported generated models through `runtime-core` XML output interfaces with namespace-correct element/attribute names, stable ordering, explicit method dispatch, and no reflection, annotations, ServiceLoader, dynamic proxy, or classpath scanning.
Tests to add/update: golden writer source tests, generated writer compile tests, XML output tests for simple elements, attributes, nested sequences, cardinality, namespace prefixes where configured, and deterministic output ordering
Documentation to update: generated-code contract, runtime architecture, conformance matrix, and traceability docs when writer behavior is implemented
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: generated writers compile and produce namespace-correct XML for supported constructs; model generation remains deterministic; no XML reader or validation behavior is added
Rollback notes: revert writer emitter source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: writer tests must include golden source and generated-code behavior.
- Native Image: generated writers must be reflection-free and statically reachable.
- Security: writers must not perform resource access.
- Documentation: conformance status may move only for writer scenarios with tests.
