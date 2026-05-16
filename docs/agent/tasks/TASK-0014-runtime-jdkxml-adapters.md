# TASK-0014: runtime-jdkxml-adapters

Status: draft.

Task ID: `TASK-0014`
Gate: Phase 4 generated reader and basic validation vertical slice; starts only after `TASK-0013` is accepted.
Requirement IDs: `REQ-RT-001`, `REQ-XML-R-001`, `REQ-XML-W-001`, `REQ-VAL-001`, `REQ-SEC-001`, `REQ-QA-001`, `REQ-NI-001`
ADR IDs: `ADR-0003`, `ADR-0005`, `ADR-0007`, `ADR-0010`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/runtime-architecture.md`, `docs/architecture/security-architecture.md`, `docs/verification/security-test-plan.md`
Target module: `modules/runtime-jdkxml`
Allowed files: `modules/runtime-jdkxml/src/main/java/io/github/mundanej/mxjb/runtime/jdkxml/**`, `modules/runtime-jdkxml/src/test/java/io/github/mundanej/mxjb/runtime/jdkxml/**`, module README updates, and directly related docs
Forbidden files: generated-code emitters, generator public API, CLI source, Gradle plugin source, dependency metadata, and any change making `runtime-jdkxml` required by generated code
Expected behavior: implement optional adapters from JDK XML APIs to `runtime-core` XML event/output interfaces for tests and examples, with secure defaults for entity/resource behavior and stable location/diagnostic propagation where available.
Tests to add/update: adapter unit tests, XML reading/writing adapter tests, denied external entity/resource tests, location propagation tests, module dependency tests, and Native Image compatibility checks where feasible
Documentation to update: runtime architecture, security architecture, module README, and traceability docs
Commands to run: `./gradlew :modules:runtime-jdkxml:test :modules:runtime-jdkxml:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: adapters are optional; generated code can depend only on `runtime-core`; network/entity behavior is denied or explicitly controlled; no generator behavior is added
Rollback notes: revert runtime-jdkxml source, tests, and directly related docs

## Impact Notes

- Coverage: adapter behavior must be covered with positive and negative XML tests.
- Native Image: adapter code must not require reflection configuration by default.
- Security: external entity and network denial tests are required.
- Documentation: docs must keep the optional-adapter distinction clear.
