# TASK-0015: generated-reader-emitter

Status: accepted.

Task ID: `TASK-0015`
Gate: Phase 4 generated reader and basic validation vertical slice; starts only after `TASK-0014` is accepted.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-NS-001`, `REQ-RT-001`, `REQ-RT-002`, `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/architecture/validation-architecture.md`
Target module: `modules/generator-core`
Allowed files: generator-core reader emitter source/tests/resources/golden reader fixtures, generated compile-test fixtures, generated reader behavior fixtures, and directly related docs
Forbidden files: runtime module product source unless a prior accepted runtime interface requires test-only usage, public `generator-api` source, CLI source, Gradle plugin source, generated validation implementation beyond basic reader diagnostics, dependency metadata, and committed generated product code outside approved golden fixtures
Expected behavior: deterministic XML reader source consumes `runtime-core` XML event readers and constructs generated model instances for supported `XP-DATA-10` constructs with explicit dispatch, namespace-aware matching, order/cardinality awareness, stable diagnostics, and no reflection or annotation-based binding.
Tests to add/update: golden reader source tests, generated reader compile tests, XML input tests for simple/nested/attribute/sequence/cardinality scenarios, namespace mismatch diagnostics, unsupported construct diagnostics, and round-trip setup fixtures
Documentation to update: generated-code contract, runtime architecture, validation architecture, conformance matrix, and traceability docs
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: generated readers compile and parse supported XML into generated models; diagnostics are deterministic; writer behavior remains stable; no public CLI or Gradle entry point is added
Rollback notes: revert reader emitter source, tests, fixtures, golden files, and directly related docs

## Impact Notes

- Coverage: reader tests must cover both source generation and generated-code behavior.
- Native Image: generated readers must be statically reachable and reflection-free.
- Security: readers must depend on controlled event input, not direct network/resource access.
- Documentation: implemented conformance rows require matching tests.

## Acceptance Record

- Added `GeneratedReaderEmitter` and `GeneratedReaderEmissionResult` in `generator-core`.
- Generated one static `<RootTypeSimpleName>XmlReader` per root element in `<model-package>.xml`.
- Generated readers target only `runtime-core` `XmlEventReader`, `XmlName`, diagnostics, and `XmlReadException` types.
- Covered required/optional/repeated elements, attributes, nested model elements, namespace matching, scalar lexical conversion, deterministic diagnostics, golden source verification, and generated-code behavior tests.
- Kept CLI, Gradle plugin, public generator API, runtime module source, and dependency metadata unchanged.
