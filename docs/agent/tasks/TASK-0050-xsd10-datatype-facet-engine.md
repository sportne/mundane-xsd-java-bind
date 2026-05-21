# TASK-0050: xsd10-datatype-facet-engine

Status: draft.

Task ID: `TASK-0050`
Gate: full XSD 1.0 datatype and facet engine.
Target areas: runtime-core value types, generator-core scalar typing, reader/writer lexical conversion, generated validation, tests, docs
Allowed files: runtime-core, runtime-jdkxml tests where needed, generator-core schema/bind/emit/tests, conformance fixtures, docs
Forbidden files: content-model automata, full derivation/polymorphism, identity constraints, external dependencies without ADR, XSD 1.1/XML 1.1
Expected behavior: implement all XML Schema 1.0 built-in simple types, derived built-ins, and restriction facets with deterministic lexical conversion and generated validation.
Tests to add/update: datatype unit tests, facet combination tests, generated reader/writer/validator tests, JUnit integration-style conformance fixtures.
Acceptance criteria: XSD 1.0 primitive and derived built-ins plus all XSD 1.0 facets are covered by executable tests or explicitly documented non-binding metadata behavior.
Rollback notes: revert runtime datatype additions, scalar binding changes, and fixture updates from this task.
