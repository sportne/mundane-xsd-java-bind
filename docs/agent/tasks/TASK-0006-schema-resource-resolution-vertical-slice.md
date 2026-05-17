# TASK-0006: schema-resource-resolution-vertical-slice

Status: accepted.

Task ID: `TASK-0006`
Gate: Phase 2 schema compiler vertical slice.
Requirement IDs: `REQ-SCOPE-001`, `REQ-SCHEMA-005`, `REQ-RES-001`, `REQ-SEC-001`, `REQ-QA-001`, `REQ-AGENT-001`
ADR IDs: `ADR-0001`, `ADR-0002`, `ADR-0005`, `ADR-0006`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/architecture/security-architecture.md`, `docs/verification/security-test-plan.md`, `docs/conformance/matrix.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/io/github/mundanej/mxjb/generator/core/resolver/**`, `modules/generator-core/src/main/java/io/github/mundanej/mxjb/generator/core/schema/**`, `modules/generator-core/src/main/java/io/github/mundanej/mxjb/generator/core/diagnostics/**`, `modules/generator-core/src/test/java/io/github/mundanej/mxjb/generator/core/**`, `modules/generator-core/src/test/resources/io/github/mundanej/mxjb/generator/core/**`, `modules/generator-core/src/test/resources/golden/schema-resolution/**`, and directly related updates to this task card, `docs/conformance/matrix.md`, `docs/requirements/traceability-matrix.md`, `docs/architecture/compiler-pipeline.md`, or `docs/architecture/security-architecture.md`
Forbidden files: runtime module product source, `generator-api` public source, CLI source, Gradle plugin source, example generated source, binding model implementation, Java source emitter implementation, generated model implementation, XML reader/writer implementation, validation engine implementation, dependency declarations, dependency locks, and dependency verification metadata
Expected behavior: implement internal `generator-core` schema source and resolver policy primitives that resolve primary schemas plus `xs:include` and `xs:import` through explicit local roots or catalog mappings; deny implicit network resolution; detect include/import cycles; emit deterministic diagnostics; and produce a stable resolved-schema manifest suitable for golden tests. Minimal XSD parsing is limited to root schema metadata and include/import discovery.
Tests to add/update: unit tests for local primary schema resolution, include resolution, import resolution, catalog mapping, denied network URI diagnostics, include/import cycle diagnostics, deterministic traversal order, and the golden resolved-schema manifest; an ArchUnit test must verify the new implementation does not introduce forbidden runtime or generated-code dependencies
Documentation to update: update conformance and traceability docs only when the implementation changes the planned status, diagnostic identifiers, or test identifiers
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: tests prove denied network access and include/import cycle detection; schema resolution behavior is deterministic; no public generator API is added; no runtime, generated-code, binding, reader, writer, or validation behavior is introduced; no new dependencies are added; quality gate and whitespace checks pass
Rollback notes: revert the new `generator-core` resolver/schema/diagnostics source, tests, fixtures, golden files, and any directly related documentation updates

## Coverage, Native Image, Security, and Documentation Impact

- Coverage: new production code must have focused unit coverage through `generator-core` tests; staged repository-wide JaCoCo thresholds remain governed by `docs/verification/coverage-policy.md`.
- Native Image: no generated sample binding exists yet, so this task does not add native-image execution; it must avoid reflection, dynamic proxies, classpath scanning, and ServiceLoader discovery in implementation code.
- Security: denied network resolution and include/import cycle diagnostics are required tests for this task.
- Documentation: keep user-facing docs unchanged unless the implementation alters documented conformance, traceability, or security behavior.
