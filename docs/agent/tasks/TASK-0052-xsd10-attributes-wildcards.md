# TASK-0052: xsd10-attributes-wildcards

Status: draft.

Task ID: `TASK-0052`
Gate: full XSD 1.0 attribute and wildcard support.
Target areas: frontend/IR attribute metadata, binding model, reader/writer/validator emitters, runtime fragments, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core if retained attribute values need small public types, conformance fixtures, docs
Forbidden files: DOM-backed binding, lexical prefix preservation, identity constraints, XSD 1.1/XML 1.1
Expected behavior: implement `xs:anyAttribute`, full wildcard namespace constraints, `processContents` skip/lax/strict policy, attribute `use` forms, defaults/fixed values, refs, and attribute wildcard composition.
Tests to add/update: attribute/wildcard unit tests, generated reader/writer/validator tests, selected conformance/interop fixtures.
Acceptance criteria: all XSD 1.0 attribute and wildcard forms are either bound and validated or classified as intentionally incompatible with generated binding.
Rollback notes: revert attribute/wildcard binding and fixture changes from this task.
