# TASK-0054: xsd10-identity-constraints

Status: accepted.

Task ID: `TASK-0054`
Gate: XSD 1.0 identity constraints and document-level validation.
Target areas: frontend/IR identity metadata, XPath subset compiler, generated validator document context, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core only for small validation context primitives if needed, conformance fixtures, docs
Forbidden files: general XPath engine dependency without ADR, XSD 1.1 assertions, XML 1.1, release metadata
Expected behavior: implement `xs:unique`, `xs:key`, and `xs:keyref` using the XSD 1.0 selector/field XPath subset and generated document-scope validation state.
Tests to add/update: XPath subset unit tests, identity table tests, key/keyref positive and negative JUnit integration-style fixtures.
Acceptance criteria: identity constraints validate consistently against accepted generated document models and preserve deterministic diagnostics.
Rollback notes: revert identity metadata, XPath subset compiler, and validator context changes from this task.

## Completion notes

- Replaced the pre-binding identity-constraint rejection with normalized IR metadata attached to
  element declarations and carried through root binding metadata without changing generated public
  model APIs.
- Added an internal accepted XPath subset compiler for selector/field paths. Supported syntax
  includes namespace-aware QName steps, `*`, `.`, `.//`, `/`, union alternatives with `|`, and
  terminal attribute fields such as `@id`; unsupported axes, predicates, functions, parent
  traversal, variables, absolute paths, and arbitrary XPath expressions remain deterministic
  schema diagnostics.
- Generated validators now emit private document-scope identity helpers for roots with
  `xs:unique`, `xs:key`, or `xs:keyref`. Object/XML validation collects selected generated model
  nodes, computes scalar key tuples, rejects complete duplicate `unique`/`key` tuples, requires
  complete `key` fields, and resolves complete `keyref` tuples after collection.
- Added unit and generated-source tests for identity metadata, unsupported XPath diagnostics,
  unresolved keyrefs, duplicate key diagnostics, and dangling keyref diagnostics.
- Added selected local conformance fixture `T-CONF-XP-XSD10-SEMANTIC-IDENTITY`, comparing JDK XML
  Schema validation with generated binding validation for valid input, duplicate keys, and dangling
  keyrefs.

## Verification

- `./gradlew :modules:generator-core:test --tests '*SchemaIrBuilderTest*' --tests '*GeneratedValidatorEmitterTest*' --console=plain` passed.
- `./gradlew :modules:conformance-tests:test --tests '*XpXsd10SemanticConformanceTest*' --tests '*SelectedConformanceFixtureManifestTest*' --console=plain` passed.
- `./gradlew :modules:generator-core:check --console=plain` passed.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain` passed.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
