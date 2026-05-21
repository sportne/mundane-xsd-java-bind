# TASK-0053: xsd10-derivation-polymorphism

Status: draft.

Task ID: `TASK-0053`
Gate: full XSD 1.0 derivation, substitution, and dynamic typing.
Target areas: component graph, IR normalization, binding model, generated model/reader/writer/validator emitters, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core only if dynamic dispatch needs public helpers, conformance fixtures, docs
Forbidden files: identity constraints, XML Canonicalization, release metadata, XSD 1.1/XML 1.1
Expected behavior: implement complex extension/restriction, simpleContent extension/restriction, full simple restriction/list/union composition, abstract types/elements, block/final semantics, full substitution groups, and known `xsi:type` dispatch.
Tests to add/update: derivation graph tests, generated sealed hierarchy tests, read/write/validate dynamic type fixtures, unsupported invalid dynamic-type diagnostics.
Acceptance criteria: generated binding supports legal XSD 1.0 derivation and polymorphism while rejecting invalid or unknown dynamic types deterministically.
Rollback notes: revert polymorphic model and derivation-normalization changes from this task.
