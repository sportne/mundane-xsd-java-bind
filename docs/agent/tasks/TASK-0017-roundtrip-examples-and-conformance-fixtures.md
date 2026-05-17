# TASK-0017: roundtrip-examples-and-conformance-fixtures

Status: accepted.

Task ID: `TASK-0017`
Gate: Phase 4 generated reader and basic validation vertical slice; starts only after `TASK-0016` is accepted.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-SCHEMA-005`, `REQ-SCHEMA-006`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-NI-001`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0010`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/verification/verification-plan.md`, `docs/verification/native-image-test-plan.md`, `docs/conformance/matrix.md`, `docs/conformance/w3c-test-suite-policy.md`, `examples/purchase-order/README.md`, `examples/multi-namespace/README.md`
Target modules: `modules/conformance-tests`, `modules/testing-support`, and examples
Allowed files: conformance test fixtures/tests, testing-support helpers needed for generated binding tests, example schema/XML/test fixtures, approved golden generated outputs for tests, module/example READMEs, and directly related docs
Forbidden files: public CLI implementation, Gradle plugin implementation, dependency metadata, broad W3C suite import, future-profile features, and generated product code outside approved golden/example/test fixtures
Expected behavior: add representative purchase-order and multi-namespace schemas, XML fixtures, generated-code test helpers, round-trip tests, negative diagnostics fixtures, and selected conformance rows for implemented `XP-DATA-10` behavior.
Tests to add/update: object-to-XML-to-object tests, XML-to-object-to-XML tests, negative XML diagnostics, multi-namespace fixture tests, include/import fixture tests, representative native smoke reuse of executable round-trip fixtures where available, and conformance matrix linkage tests where practical
Documentation to update: example READMEs, conformance matrix, verification plan, traceability, and W3C test-suite policy notes as needed
Commands to run: `./gradlew :modules:conformance-tests:test :examples:purchase-order:check :examples:multi-namespace:check`, representative native smoke command when round-trip fixtures are executable or documented blocker, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: representative generated bindings round-trip through generated reader/writer paths; executable representative round trips are included in native smoke coverage where GraalVM is available or the blocker is documented; unsupported conformance areas remain future or unsupported-by-design; no user-facing generator command is added
Rollback notes: revert conformance/example/testing-support source, fixtures, generated golden/example outputs, and related docs

Completion notes: `TASK-0017` added purchase-order and multi-namespace checked-in generated-style
fixture sources, XML/schema fixtures, JVM round-trip checks, selected `XP-DATA-10` conformance
fixtures, and example Native Image round-trip smoke execution. Repeated namespaced sibling fixtures
also exposed and fixed a namespace declaration scope defect in the optional JDK XML output adapter.

## Impact Notes

- Coverage: examples and conformance tests must raise confidence in complete vertical behavior.
- Native Image: representative executable round-trip fixtures must run in, or be staged for, native smoke coverage; `TASK-0020` hardens and broadens this lane.
- Security: no untagged tests may access the network.
- Documentation: examples must not imply unsupported full-XSD behavior.
