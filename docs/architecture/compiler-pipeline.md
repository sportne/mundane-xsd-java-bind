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

## `TASK-0027` composed-schema normalization

The `XP-XSD10-COMPOSED` profile keeps the same compiler stages. `TASK-0027` implements named model
groups and attribute groups by normalizing accepted composition constructs before binding so
generated code can preserve the existing explicit model, reader, writer, and validator architecture.

- Accepted named model groups and attribute groups are resolved from the component graph and flattened into
  containing complex types during normalization.
- `TASK-0028` list/union simple types are resolved as named simple-type aliases that carry
  item/member metadata into binding and validation planning.
- Planned complex extension and simple restriction derivation chains are flattened or merged before
  emission; generated Java inheritance is not part of the planned model shape.
- Recursive groups or derivation chains, unsupported composition depth, and out-of-scope constructs
  must fail with deterministic diagnostics before source emission.
