# generator-api

Public generator configuration and extension API.

## Current status

`generator-api` exposes the first stable schema-to-Java entry point for the accepted
`XP-DATA-10` vertical slice. The API is intentionally small:

- `Generator` accepts a `GeneratorRequest` and returns a `GeneratorResult`.
- `GeneratorRequest` carries schema paths, output directory, profile, default package,
  namespace/package mappings, local roots, and catalog mappings.
- `GeneratorResult` reports generated relative source paths and public diagnostics.
- `GeneratorDiagnostic` uses stable `code`, `resource`, and `message` fields.
- `GeneratorProfile.XP_DATA_10` is the first supported profile and maps to the CLI token
  `XP-DATA-10`.

The API does not expose parser, IR, binding, or emitter implementation types.

## Usage shape

```java
Generator generator = new io.github.mundanej.mxjb.generator.core.CoreGenerator();
GeneratorRequest request = GeneratorRequest.of(
    java.util.List.of(java.nio.file.Path.of("schemas/order.xsd")),
    java.nio.file.Path.of("build/generated/mxjb"));
GeneratorResult result = generator.generate(request);
```

## Contributor notes

- Do not expose implementation parser or compiler internals.
- Keep API additions tied to requirement IDs and ADR review.
- Add package documentation for every public package.
