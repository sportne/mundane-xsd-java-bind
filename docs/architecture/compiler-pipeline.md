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
