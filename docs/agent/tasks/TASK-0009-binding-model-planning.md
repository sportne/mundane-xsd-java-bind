# TASK-0009: binding-model-planning

Status: draft.

Task ID: `TASK-0009`
Gate: Phase 2 schema compiler vertical slice; starts only after `TASK-0008` is accepted.
Requirement IDs: `REQ-NS-001`, `REQ-GEN-001`, `REQ-MODEL-001`, `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-VAL-002`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/bind/**`, `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/diagnostics/**`, `modules/generator-core/src/test/java/io/github/xsdbind/generator/core/**`, `modules/generator-core/src/test/resources/**`, and directly related traceability/conformance docs
Forbidden files: runtime modules, public `generator-api` source, CLI source, Gradle plugin source, Java source emitter implementation, XML reader/writer implementation, validation engine implementation, and dependency metadata
Expected behavior: transform normalized schema IR into an internal binding model containing deterministic Java package names, type names, field names, collection shapes, writer/reader planning metadata, and basic validation-plan metadata for supported `XP-DATA-10` constructs.
Tests to add/update: golden binding-model tests for namespace-to-package mapping, name collision handling, records versus final classes/builders where planned, repeated/optional field shapes, attribute and element fields, validation-plan metadata, and deterministic diagnostics
Documentation to update: traceability and conformance docs when binding decisions or test identifiers become implemented
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: binding model is deterministic; generated-code style decisions are represented but no Java source is emitted; `xs:choice` remains profile-gated unless a later task approves it; no public generator API is added
Rollback notes: revert binding model source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: tests must exercise binding decisions through schema fixtures, not only isolated name helpers.
- Native Image: no native execution added.
- Security: no additional resource access is introduced.
- Documentation: any binding decision that changes public generated-code shape must be reflected before source emission tasks begin.
