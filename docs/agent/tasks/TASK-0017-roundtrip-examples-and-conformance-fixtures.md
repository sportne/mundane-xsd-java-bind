# TASK-0017: roundtrip-examples-and-conformance-fixtures

Status: draft.

Task ID: `TASK-0017`
Gate: Phase 4 generated reader and basic validation vertical slice; starts only after `TASK-0016` is accepted.
Requirement IDs: `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-SCHEMA-005`, `REQ-SCHEMA-006`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-001`, `REQ-NI-001`
ADR IDs: `ADR-0005`, `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0010`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/verification/verification-plan.md`, `docs/conformance/matrix.md`, `docs/conformance/w3c-test-suite-policy.md`, `examples/purchase-order/README.md`, `examples/multi-namespace/README.md`
Target modules: `modules/conformance-tests`, `modules/testing-support`, and examples
Allowed files: conformance test fixtures/tests, testing-support helpers needed for generated binding tests, example schema/XML/test fixtures, approved golden generated outputs for tests, module/example READMEs, and directly related docs
Forbidden files: public CLI implementation, Gradle plugin implementation, dependency metadata, broad W3C suite import, future-profile features, and generated product code outside approved golden/example/test fixtures
Expected behavior: add representative purchase-order and multi-namespace schemas, XML fixtures, generated-code test helpers, round-trip tests, negative diagnostics fixtures, and selected conformance rows for implemented `XP-DATA-10` behavior.
Tests to add/update: object-to-XML-to-object tests, XML-to-object-to-XML tests, negative XML diagnostics, multi-namespace fixture tests, include/import fixture tests, and conformance matrix linkage tests where practical
Documentation to update: example READMEs, conformance matrix, verification plan, traceability, and W3C test-suite policy notes as needed
Commands to run: `./gradlew :modules:conformance-tests:test :examples:purchase-order:check :examples:multi-namespace:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: representative generated bindings round-trip through generated reader/writer paths; unsupported conformance areas remain future or unsupported-by-design; no user-facing generator command is added
Rollback notes: revert conformance/example/testing-support source, fixtures, generated golden/example outputs, and related docs

## Impact Notes

- Coverage: examples and conformance tests must raise confidence in complete vertical behavior.
- Native Image: fixtures should be ready for smoke-test reuse in `TASK-0020`.
- Security: no untagged tests may access the network.
- Documentation: examples must not imply unsupported full-XSD behavior.
