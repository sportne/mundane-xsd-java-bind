# TASK-0049: xsd10-frontend-component-model

Status: draft.

Task ID: `TASK-0049`
Gate: full XSD 1.0 frontend and component model.
Target areas: syntax parser, component graph, resolver, IR diagnostics, conformance fixtures, docs
Allowed files: generator-core frontend/component graph/IR tests and implementation, local fixtures, docs
Forbidden files: datatype engine rewrite, content-model automata, identity constraints, release metadata, XSD 1.1/XML 1.1
Expected behavior: parse and represent remaining XSD 1.0 declarations and component metadata, including annotations, notations, redefine, chameleon includes, form defaults, block/final defaults, and schema symbol spaces, while rejecting unimplemented binding behavior deterministically.
Tests to add/update: frontend and component graph unit tests plus selected local fixtures for every newly represented construct.
Acceptance criteria: every XSD 1.0 schema/component construct in the feature matrix is either represented or has a deterministic diagnostic before binding.
Rollback notes: revert component-model additions and fixture classifications from this task.
