# TASK-0054: xsd10-identity-constraints

Status: draft.

Task ID: `TASK-0054`
Gate: XSD 1.0 identity constraints and document-level validation.
Target areas: frontend/IR identity metadata, XPath subset compiler, generated validator document context, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core only for small validation context primitives if needed, conformance fixtures, docs
Forbidden files: general XPath engine dependency without ADR, XSD 1.1 assertions, XML 1.1, release metadata
Expected behavior: implement `xs:unique`, `xs:key`, and `xs:keyref` using the XSD 1.0 selector/field XPath subset and generated document-scope validation state.
Tests to add/update: XPath subset unit tests, identity table tests, key/keyref positive and negative JUnit integration-style fixtures.
Acceptance criteria: identity constraints validate consistently against accepted generated document models and preserve deterministic diagnostics.
Rollback notes: revert identity metadata, XPath subset compiler, and validator context changes from this task.
