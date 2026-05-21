# TASK-0051: xsd10-content-model-compiler

Status: draft.

Task ID: `TASK-0051`
Gate: full XSD 1.0 particle and content-model support.
Target areas: IR particles, binding model, generated reader/validator emitters, generated model shapes, tests, docs
Allowed files: generator-core schema/bind/emit/tests, conformance fixtures, generated-code smoke fixtures, docs
Forbidden files: datatype expansion beyond prerequisites, identity constraints, release metadata, XSD 1.1/XML 1.1
Expected behavior: compile nested `sequence`, `choice`, `all`, repeated/optional model groups, wildcard particles, cardinality composition, and UPA checks into shared reader/validator content-model plans.
Tests to add/update: content-model automata unit tests, generated reader/validator edge cases, repeated/nested choice model-shape tests, JUnit integration-style fixtures.
Acceptance criteria: read-time and object-validation content-model behavior agree across all XSD 1.0 model group forms.
Rollback notes: revert content-model compiler and generated-shape changes from this task.
