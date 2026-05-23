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
- `TASK-0029` complex extension and simple restriction derivation chains are flattened or merged
  before emission; generated Java inheritance is not part of the accepted model shape.
- `TASK-0053` adds simpleContent value metadata for accepted text-with-attributes models and
  performs basic complex restriction member checks before binding. Complete XSD restriction algebra
  remains a later full-XSD task.
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
- Substitution cycles, unsupported blocking/final semantics, and unsupported semantic combinations
  must fail with deterministic diagnostics before source emission.

## `TASK-0037` document/open-content normalization

The `XP-XSD10-DOCUMENT` profile keeps open content inside the existing compiler stages
instead of introducing DOM-backed runtime binding.

- `TASK-0037` carries accepted direct `xs:any` particles through frontend, component graph, IR,
  binding, and emission as explicit wildcard fields with namespace constraint metadata.
- `TASK-0037` rejects unsupported wildcard locations and `processContents` modes before source
  emission, including wildcard choices, substitution-branch wildcards, `xs:anyAttribute`, and
  `processContents="lax"` or `"strict"`.
- `TASK-0038` carries accepted `mixed="true"` metadata into binding as an ordered generated
  content-list model rather than separate unordered element fields.
- `TASK-0039` verifies deterministic serialization policy for generated output and retained
  fragments without claiming formal XML Canonicalization.
