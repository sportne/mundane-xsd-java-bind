# TASK-0007: xsd-syntax-frontend-subset

Status: approved.

Task ID: `TASK-0007`
Gate: Phase 2 schema compiler vertical slice; starts after accepted `TASK-0006`.
Requirement IDs: `REQ-SCOPE-001`, `REQ-SCHEMA-001`, `REQ-SCHEMA-002`, `REQ-SCHEMA-003`, `REQ-SCHEMA-004`, `REQ-SCHEMA-006`, `REQ-SCHEMA-007`, `REQ-QA-001`, `REQ-AGENT-001`
ADR IDs: `ADR-0001`, `ADR-0002`, `ADR-0005`, `ADR-0006`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/compiler-pipeline.md`, `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/standards-baseline.md`
Target module: `modules/generator-core`
Allowed files: `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/schema/**`, `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/diagnostics/**`, narrow `modules/generator-core/src/main/java/io/github/xsdbind/generator/core/resolver/**` edits only to expose resolver-approved source paths, `modules/generator-core/src/test/java/io/github/xsdbind/generator/core/**`, `modules/generator-core/src/test/resources/io/github/xsdbind/generator/core/**`, `modules/generator-core/src/test/resources/golden/schema-frontend/**`, and directly related traceability/conformance docs
Forbidden files: runtime module product source, public `generator-api` source, CLI source, Gradle plugin source, generated-code emitters, binding model implementation, XML reader/writer implementation, validation engine implementation, and dependency metadata
Expected behavior: parse resolved XSD documents into an internal syntax model for `xs:schema`, `xs:element`, `xs:complexType`, `xs:simpleType` references, `xs:attribute`, `xs:sequence`, `minOccurs`, `maxOccurs`, target namespaces, imports/includes already resolved by `TASK-0006`, and explicit unsupported-profile diagnostics for `xs:choice` and out-of-profile constructs.
Tests to add/update: frontend unit tests and golden syntax-model fixtures for simple element, complex type, attributes, sequence, cardinality, namespace declarations, multi-document resolved input, unsupported `xs:choice`, and unsupported future-profile constructs
Documentation to update: conformance and traceability rows when test identifiers or supported syntax status change
Commands to run: `./gradlew :modules:generator-core:test :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: supported syntax fixtures produce deterministic internal model output; unsupported constructs produce deterministic diagnostics; no binding, code emission, runtime, reader, writer, or validation behavior is introduced
Rollback notes: revert frontend source, tests, fixtures, golden files, and directly related documentation updates

## Impact Notes

- Coverage: focused generator-core tests are required for each supported syntax construct.
- Native Image: no native execution added.
- Security: parsing must consume only resolver-provided local content and must not add network access.
- Documentation: public docs remain scope-controlled and must not claim full XSD support.
