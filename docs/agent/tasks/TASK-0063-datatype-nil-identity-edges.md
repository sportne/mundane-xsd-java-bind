# TASK-0063: datatype-nil-identity-edges

Status: accepted.

Task ID: `TASK-0063`
Gate: remaining datatype, nil, and identity-validation edges.
Target areas: datatype/list/union composition, NOTATION semantics, `xsi:nil` interactions,
identity-constraint validation, generator-core tests, runtime-core tests where needed, and
conformance fixtures.
Allowed files: runtime-core and generator-core production/tests, conformance fixtures/tests,
generated-code smoke fixtures, and docs for datatype/validation updates.
Forbidden files: release workflow, version bumps, publication behavior, dependencies, XSD 1.1,
XML 1.1, canonical XML, DOM-backed binding, or enabling `XP-XSD10-FULL`.
Expected behavior: close remaining XSD 1.0 validation edges needed before W3C generated-binding row
mapping, including anonymous/nested list and union composition, NOTATION value semantics,
`xsi:nil` interactions with defaults/fixed/derivation/cardinality, and identity-constraint edge
cases for generated model shapes.
Tests to add/update: runtime datatype unit tests; generator syntax/IR/binding/emitter tests;
generated reader/writer/validator tests for nil and identity interactions; selected JDK XML Schema
comparison fixtures.
Commands to run: `./gradlew :modules:runtime-core:check :modules:generator-core:check
:modules:conformance-tests:check --console=plain`, `./gradlew :modules:generator-core:generatedCodeSmoke
--console=plain`, `./gradlew validateDesignControlPack qualityGate --console=plain`,
`git diff --check`.
Acceptance criteria: all feature-matrix datatype/nil/identity blockers needed for W3C mapping are
either supported with tests or explicitly reclassified as product-scope-incompatible/non-goals with
accepted rationale; `XP-XSD10-FULL` remains non-executable.
Rollback notes: revert datatype/nil/identity changes, fixtures, docs, and task status.

## Completion notes

`TASK-0063` closes the remaining pre-mapping edges that were practical within the generated-binding
model without introducing new runtime public APIs. Named `xs:list` and `xs:union` simple types now
accept anonymous `xs:simpleType/xs:restriction` members and carry those facets through IR, binding,
generated readers, and generated validators. Nested list/union composition remains outside the
accepted Java model shape and stays explicit in the feature matrix rather than being silently
claimed.

Generated identity-node construction now treats `xsi:nil="true"` scalar element values as present
elements with no typed field value. This preserves XSD identity-constraint behavior for accepted
generated model shapes: nilled fields are ignored for `unique`, are missing for `key`, and do not
produce keyref tuples. Runtime NOTATION handling remains the accepted QName-compatible lexical
value semantics from `TASK-0050`; notation declaration tables are tolerated metadata and not exposed
as a generated public API.

No release workflow, version bump, publication behavior, dependency change, XSD 1.1/XML 1.1
support, DOM-backed binding, canonical XML behavior, or `XP-XSD10-FULL` execution is added.

## Verification evidence

- `./gradlew :modules:generator-core:test --tests '*SchemaIrBuilderTest.buildsIrForAnonymousListAndUnionRestrictionMembers' --tests '*BindingModelBuilderTest.bindsAnonymousListAndUnionRestrictionMembersForComposedProfile' --tests '*GeneratedValidatorEmitterTest.generatedValidatorTreatsNilledIdentityFieldAsMissing' --console=plain`
- `./gradlew :modules:conformance-tests:test --tests '*XpXsd10ComposedConformanceTest.anonymousListUnionFixturesMatchJdkSchemaValidationAndGeneratedBindings' --console=plain`
- `./gradlew :modules:runtime-core:check :modules:generator-core:check :modules:conformance-tests:check --console=plain`
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
