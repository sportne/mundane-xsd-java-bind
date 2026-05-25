# Compiler pipeline

## Stages

1. **Input collection**: primary schema files, binding config, catalogs, and profile selection.
2. **Resource resolution**: resolve includes/imports with deny-by-default network policy.
3. **Frontend parsing**: parse XSD into raw schema syntax model.
4. **Component graph**: resolve QName references, symbol spaces, imports/includes, and schema components.
5. **Normalization**: produce IR suitable for binding decisions.
6. **Binding**: decide Java packages, type names, field names, collection shapes, choice representations, and validation plans.
7. **Emission**: write deterministic Java source and metadata.
8. **Verification**: compile, static-analyze, golden compare, round-trip, validate, and native-image test.

## Determinism requirements

- Stable traversal order.
- Stable diagnostics order.
- Stable source formatting.
- Stable generated package/type/member names.
- Stable namespace prefix policy for generated output unless configured otherwise.

## `TASK-0027` through `TASK-0029` composed-schema normalization

The `XP-XSD10-COMPOSED` profile keeps the same compiler stages. Accepted composition constructs are
normalized before binding so generated code can preserve the existing explicit model, reader,
writer, and validator architecture.

- Accepted named model groups and attribute groups are resolved from the component graph and flattened into
  containing complex types during normalization.
- `TASK-0028` list/union simple types are resolved as named simple-type aliases that carry
  item/member metadata into binding and validation planning.
- `TASK-0063` allows anonymous simple restriction members inside accepted `xs:list` and `xs:union`
  declarations and carries those restriction facets into binding.
- `TASK-0029` complex extension and simple restriction derivation chains are flattened or merged
  before emission; generated Java inheritance is not part of the accepted model shape.
- `TASK-0053` adds simpleContent value metadata for accepted text-with-attributes models and
  performs basic complex restriction member checks before binding. Complete XSD restriction algebra
  remains a later full-XSD task.
- `TASK-0061` preserves derivation base/kind, abstract complex type metadata, and block/final
  controls in IR so binding can emit known declared-base dynamic branches without enabling
  `XP-XSD10-FULL`.
- Recursive groups or derivation chains, unsupported composition depth, and out-of-scope constructs
  must fail with deterministic diagnostics before source emission.

## `TASK-0032` semantic normalization

The `XP-XSD10-SEMANTIC` profile keeps semantic expansion inside the same compiler stages.

- `TASK-0032` carries accepted `nillable`, `default`, and `fixed` metadata through IR and binding so
  generated readers, writers, and validators can emit explicit code.
- `TASK-0033` resolves direct substitution group heads and members in normalized IR before binding,
  then emits an explicit sealed branch model instead of runtime polymorphic lookup.
- `TASK-0053` expands normalized substitution groups to accepted abstract heads, nested members,
  and repeated head references. Branches are still generated statically from known declarations.
- `TASK-0061` adds deterministic blocked substitution and final derivation diagnostics, then emits
  static known `xsi:type` branches for declared complex-base fields.
- Substitution cycles and unsupported semantic combinations must fail with deterministic diagnostics
  before source emission.

## `TASK-0037` document/open-content normalization

The `XP-XSD10-DOCUMENT` profile keeps open content inside the existing compiler stages
instead of introducing DOM-backed runtime binding.

- `TASK-0037` carries accepted direct `xs:any` particles through frontend, component graph, IR,
  binding, and emission as explicit wildcard fields with namespace constraint metadata.
- `TASK-0037` rejects unsupported wildcard locations and `processContents` modes before source
  emission, including substitution-branch wildcards, `xs:anyAttribute`, and
  `processContents="lax"` or `"strict"` before later gates broaden those shapes.
- `TASK-0038` carries accepted `mixed="true"` metadata into binding as an ordered generated
  content-list model rather than separate unordered element fields.
- `TASK-0039` verifies deterministic serialization policy for generated output and retained
  fragments without claiming formal XML Canonicalization.
- `TASK-0059` keeps repeated/optional multi-particle groups whose child particles are singleton
  particles, optional all-groups with required children, mixed choices, and wildcard choices as
  explicit grouped IR particles or content-list binding metadata instead of flattening them into
  misleading independent fields.
- `TASK-0060` extends grouped content metadata with deterministic position lists so nested choice
  alternatives share one sequence step in both generated reader and validator plans.
- `TASK-0062` binds schema-known global element and attribute declarations that match accepted
  wildcard namespace constraints so generated readers and validators can enforce strict/lax
  retained wildcard validation without DOM-backed binding or lexical-prefix preservation.
