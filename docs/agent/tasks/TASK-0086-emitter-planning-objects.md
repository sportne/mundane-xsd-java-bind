# TASK-0086: emitter-planning-objects

Status: accepted.

Task ID: `TASK-0086`
Priority: P2
Gate: generator architecture refactor.
Requirement IDs: `REQ-GEN-001`, `REQ-GEN-002`, `REQ-QA-002`
ADR IDs: `ADR-0004`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/requirements/traceability-matrix.md`
Target module: `modules/generator-core`
Allowed files: generator-core generated emitter planning/helper classes and tests, architecture
docs, traceability docs, and this task/handoff.
Forbidden files: public API changes, broad emitter traversal rewrites, generated source behavior
changes, reflection/runtime dependency additions, release metadata, dependency metadata, and
quality-gate weakening.
Expected behavior: introduce package-private reader/writer/validator planning objects before source
text assembly. Plans should capture root helper names, target source names, root/type metadata, and
validated field traversal inputs while preserving generated Java source output.
Tests to add/update: focused plan characterization tests plus existing generated-source
golden/compile/smoke coverage. If any generated source text changes, document it as formatting-only
and keep compile/smoke evidence current.
Documentation to update: complexity review, generated-code contract if planning vocabulary is
clarified, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
targeted conformance checks if generated fixtures or conformance behavior changes,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: emitters build explicit package-private plans before source text assembly;
generated output behavior is unchanged; no broad emitter traversal refactor or public API change is
introduced.
Rollback notes: revert planning objects, tests, and docs.

Completion notes:
- Added package-private `GeneratedEmitterPlan`, `GeneratedEmitterKind`, and
  `GeneratedEmitterFieldPlan` in the emitter package.
- Reader, writer, and validator emitters now resolve a root plan before source text assembly. The
  plan captures the root element, root binding type, generated source type name, source path, root
  helper name, and binding-order field traversal inputs.
- Kept existing source-state traversal and text assembly logic intact; the only source-emitter
  mechanical cleanup uses `XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI` for existing XSI namespace
  constants to avoid an Error Prone analyzer crash on string literals.
- Added focused plan tests for reader/writer/validator source names, helper names, relative paths,
  root metadata, and field traversal inputs.

Evidence:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
