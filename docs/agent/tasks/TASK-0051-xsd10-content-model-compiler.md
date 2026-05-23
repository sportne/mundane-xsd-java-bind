# TASK-0051: xsd10-content-model-compiler

Status: accepted.

Task ID: `TASK-0051`
Gate: accepted XSD 1.0 particle and content-model expansion toward `XP-XSD10-FULL`.
Target areas: IR particles, binding model, generated reader/validator emitters, generated model shapes, tests, docs
Allowed files: generator-core schema/bind/emit/tests, conformance fixtures, generated-code smoke fixtures, docs
Forbidden files: datatype expansion beyond prerequisites, identity constraints, release metadata, XSD 1.1/XML 1.1
Expected behavior: compile the accepted content-model subset into deterministic reader, writer, and validator behavior: required/all-optional `xs:all`, nested singleton sequences, single-particle repeated/optional group refs with composed cardinality, and repeated element-only choices; unsupported grouped-list and complete automata cases remain deterministic diagnostics.
Tests to add/update: content-model automata unit tests, generated reader/validator edge cases, repeated/nested choice model-shape tests, JUnit integration-style fixtures.
Acceptance criteria: read-time and object-validation behavior agree for the accepted content-model subset; optional all-groups with required children, repeated/optional multi-particle groups, complete grouped-list semantics, and complete UPA automata remain future work.
Rollback notes: revert content-model compiler and generated-shape changes from this task.

Completion notes:
- Added normalized `xs:all` IR support for required all-groups and optional all-groups whose members are all optional, including unordered generated reader behavior by assigning all member fields the same content order and deterministic writer output in binding order.
- Added nested singleton sequence flattening, cardinality composition for single-particle nested sequences and group refs, and stable diagnostics for repeated/optional multi-particle groups that require a future grouped content-list shape.
- Added repeated `xs:choice` support through generated `List<<ContainingTypeSimpleName>Choice>` fields, sealed choice branch records, generated reader dispatch, writer emission, and validator checks.
- Preserved current wildcard semantics and existing mixed-content whitespace policy; optional `xs:all` groups with required children, full wildcard namespace algebra, `processContents=lax|strict`, derivation/polymorphism, and identity constraints remain deferred.

Verification evidence:
- `./gradlew :modules:generator-core:check --console=plain` passed.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain` passed.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
