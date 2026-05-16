# TASK-0010: runtime-core-primitives

Status: draft.

Task ID: `TASK-0010`
Gate: Phase 3 generated model and writer vertical slice; starts only after `TASK-0009` is accepted.
Requirement IDs: `REQ-RT-001`, `REQ-RT-002`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-QA-001`, `REQ-NI-001`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0005`, `ADR-0007`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/runtime-architecture.md`, `docs/architecture/module-boundaries.md`, `docs/architecture/native-image-architecture.md`
Target module: `modules/runtime-core`
Allowed files: `modules/runtime-core/src/main/java/io/github/xsdbind/runtime/**`, `modules/runtime-core/src/test/java/io/github/xsdbind/runtime/**`, module README updates, and directly related architecture/traceability docs
Forbidden files: generator implementation source, `runtime-jdkxml` source, CLI source, Gradle plugin source, generated example source, dependency declarations, dependency locks, and dependency verification metadata
Expected behavior: add dependency-free runtime-core primitives needed by generated code: XML names, source locations, diagnostics, read/write exceptions, XML event reader/output interfaces, and validation result/error value types.
Tests to add/update: unit tests for value semantics, null/argument invariants, diagnostic stability, exception data retention, no-third-party dependency enforcement, ArchUnit module-boundary rules, and Native Image execution for runtime-core primitives once behavior exists
Documentation to update: runtime architecture, module README, traceability, and conformance docs as needed
Commands to run: `./gradlew :modules:runtime-core:test :modules:runtime-core:check`, `./gradlew :modules:runtime-core:nativeTest` when GraalVM is available or document the concrete blocker, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: runtime-core has no third-party dependencies; generated-code-facing interfaces are small and explicit; no XML parser adapter is added; no reflection, ServiceLoader, dynamic proxy, or classpath scanning is used
Rollback notes: revert runtime-core source, tests, and directly related documentation updates

## Impact Notes

- Coverage: runtime-core code must meet the staged coverage policy once production classes exist.
- Native Image: interfaces and value types must be Native Image friendly by default; this task must begin mechanical native execution once runtime-core behavior is meaningful.
- Security: no resource resolution or XML parsing is added here.
- Documentation: public runtime concepts require package documentation before later release readiness.
