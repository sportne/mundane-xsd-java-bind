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
- `--profile XP-DATA-10`: optional profile selection. `XP-DATA-10` is the only accepted profile.
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
