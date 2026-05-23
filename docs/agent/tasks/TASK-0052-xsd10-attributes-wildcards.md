# TASK-0052: xsd10-attributes-wildcards

Status: accepted.

Task ID: `TASK-0052`
Gate: accepted XSD 1.0 attribute and wildcard expansion toward `XP-XSD10-FULL`.
Target areas: frontend/IR attribute metadata, binding model, reader/writer/validator emitters, runtime fragments, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core if retained attribute values need small public types, conformance fixtures, docs
Forbidden files: DOM-backed binding, lexical prefix preservation, identity constraints, XSD 1.1/XML 1.1
Expected behavior: implement `xs:anyAttribute`, full wildcard namespace constraints, `processContents` skip/lax/strict policy, attribute `use` forms, defaults/fixed values, refs, and attribute wildcard composition.
Tests to add/update: attribute/wildcard unit tests, generated reader/writer/validator tests, selected conformance/interop fixtures.
Acceptance criteria: all XSD 1.0 attribute and wildcard forms are either bound and validated or classified as intentionally incompatible with generated binding.
Rollback notes: revert attribute/wildcard binding and fixture changes from this task.

Completion notes:
- Added local attribute namespace qualification with `attributeFormDefault` and per-attribute `form`; global attributes remain target-namespace qualified.
- Added `xs:anyAttribute` binding as generated immutable `List<XmlAttribute>` fields, with reader capture, writer emission in list order, validator namespace checks, and prohibited-attribute exclusion.
- Added `use="prohibited"` handling as non-public binding metadata that rejects matching XML attributes while preserving normal declared attributes and defaults/fixed semantics.
- Added wildcard `processContents` metadata for `skip`, `lax`, and `strict`, defaulting omitted values to `strict`, and broadened namespace-token handling for `##local`, `##targetNamespace`, explicit URI lists, `##any`, and `##other`.
- Preserved retained `XmlFragment` element wildcard behavior; full strict/lax schema-known deep validation, wildcard choices, derivation interactions, and identity constraints remain future gates.

Verification evidence:
- `./gradlew :modules:generator-core:check --console=plain` passed.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain` passed.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
