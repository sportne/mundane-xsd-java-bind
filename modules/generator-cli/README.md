# generator-cli

Command-line generator entry point.

## Current status

`generator-cli` provides the first public command-line surface for the accepted
schema-to-Java vertical slice.

```bash
mxjb generate --schema examples/purchase-order/src/main/resources/schema/purchase-order.xsd --output build/generated/mxjb-cli
```

Supported options:

- `--schema <path>`: primary schema path. Repeat for multiple primary schemas.
- `--output <dir>`: required generated-source output directory.
- `--profile <XP-DATA-10|XP-DATA-10-CHOICE|XP-VALIDATION-10-BASIC|XP-XSD10-COMPOSED|XP-XSD10-SEMANTIC>`: optional profile selection.
  `XP-DATA-10-CHOICE` enables the accepted opt-in `xs:choice` subset;
  `XP-VALIDATION-10-BASIC` enables accepted named simple restriction facets;
  `XP-XSD10-COMPOSED` enables the accepted composed profile subset: named group and
  attribute-group flattening, named list/union simple types, and initial derivation flattening;
  `XP-XSD10-SEMANTIC` enables accepted nillable/default/fixed, direct substitution-group, and
  generated semantic validation behavior.
- `--default-package <package>`: package used when a namespace-specific mapping is absent.
- `--namespace-package <namespace=package>`: explicit namespace-to-Java-package mapping.
- `--local-root <dir>`: additional local schema resolution root.
- `--catalog <uri=path>`: catalog mapping for imports/includes.
- `--help`: print usage.

Successful generation prints sorted generated relative paths to stdout. CLI argument errors
exit with code `2`; generation diagnostics exit with code `1` and are printed to stderr as
`code | resource | message`.

## Contributor notes

- CLI behavior must be backed by generator API/core tests.
- Keep command examples in README and docs synchronized.
