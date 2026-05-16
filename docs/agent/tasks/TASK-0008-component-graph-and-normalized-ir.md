# TASK-0008: component-graph-and-normalized-ir

Status: approved.

Task ID: `TASK-0008`
Gate: Phase 2 schema compiler vertical slice; starts after accepted `TASK-0007`.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-SCHEMA-005`, `REQ-SCHEMA-006`, `REQ-NS-001`, `REQ-QA-001`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0009`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/module-boundaries.md`, `docs/conformance/matrix.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/schema/**`, `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/diagnostics/**`, `modules/generator-core/src/test/java/io/github/xsdbind/generator/core/**`, `modules/generator-core/src/test/resources/**`, coverage readiness edits in `build-logic/src/main/groovy/xsdbind.coverage-conventions.gradle`, directly related generator-core module docs, and directly related traceability/conformance/coverage docs
Forbidden files: runtime modules, public `generator-api` source, CLI source, Gradle plugin source, code emitters, XML reader/writer implementation, validation engine implementation, and dependency metadata
Expected behavior: resolve supported schema symbols and QNames across resolved documents, build an internal component graph, normalize supported constructs into a stable schema IR, and produce deterministic diagnostics for missing names, namespace conflicts, duplicate declarations, and out-of-profile references.
Tests to add/update: coverage-readiness tests for existing generator-core code; golden IR tests for simple elements, complex types, nested elements, attributes, sequence ordering, cardinality, namespaces, imports/includes, duplicate names, unresolved references, and deterministic diagnostic ordering
Documentation to update: traceability and conformance docs if implementation changes status or test identifiers
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: normalized IR is deterministic for equivalent input; QName and namespace behavior matches `ADR-0009`; unsupported or invalid schemas do not silently partially interpret; no public API or generated source behavior is added
Rollback notes: revert component graph and IR source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: graph and IR behavior must be tested through public module-level seams inside generator-core; generator-core coverage thresholds must be active before implementation is complete.
- Native Image: no native execution added.
- Security: all referenced schema documents must come from the approved resolver output.
- Documentation: status stays "implemented" only for features with tests.
