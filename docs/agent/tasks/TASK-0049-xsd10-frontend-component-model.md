# TASK-0049: xsd10-frontend-component-model

Status: accepted.

Task ID: `TASK-0049`
Gate: full XSD 1.0 frontend and component model.
Target areas: syntax parser, component graph, resolver, IR diagnostics, conformance fixtures, docs
Allowed files: generator-core frontend/component graph/IR tests and implementation, local fixtures, docs
Forbidden files: datatype engine rewrite, content-model automata, identity constraints, release metadata, XSD 1.1/XML 1.1
Expected behavior: parse and represent remaining XSD 1.0 declarations and component metadata, including annotations, notations, redefine, chameleon includes, form defaults, block/final defaults, and schema symbol spaces, while rejecting unimplemented binding behavior deterministically.
Tests to add/update: frontend and component graph unit tests plus selected local fixtures for every newly represented construct.
Acceptance criteria: every XSD 1.0 schema/component construct in the feature matrix is either represented or has a deterministic diagnostic before binding.
Rollback notes: revert component-model additions and fixture classifications from this task.

## Completion notes

Accepted in this task:

- XSD syntax parsing now preserves schema defaults, annotations, appinfo/documentation, notations,
  include/import/redefine nodes, `xs:all`, `xs:anyAttribute`, identity constraints, selector/field
  syntax, and abstract/block/final metadata.
- The syntax model records effective namespaces for direct and transitive chameleon includes so
  included schemas without `targetNamespace` are interpreted under the including schema namespace,
  with deterministic diagnostics for ambiguous or conflicting include namespace adoption.
- The component graph indexes notations and uses effective namespaces for chameleon-included
  declarations.
- Newly recognized but still unimplemented constructs fail deterministically during IR
  normalization before binding/source emission.
- `XP-XSD10-FULL` remains a public planned token and remains non-executable through
  `CoreGenerator`.

No datatype engine, content-model automata, wildcard/attribute behavior, derivation/polymorphism,
identity-constraint validation, runtime public API, release metadata, XSD 1.1, or XML 1.1 behavior
was added.

## Verification evidence

- `./gradlew :modules:generator-core:check --console=plain` - passed.
- `./gradlew :modules:conformance-tests:check --console=plain` - passed.
- `./gradlew validateDesignControlPack qualityGate --console=plain` - passed.
- `git diff --check` - passed.
