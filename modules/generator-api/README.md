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
- `GeneratorProfile` exposes executable profile tokens: `XP-DATA-10`, `XP-DATA-10-CHOICE`,
  `XP-VALIDATION-10-BASIC`, `XP-XSD10-COMPOSED`, `XP-XSD10-SEMANTIC`, and
  `XP-XSD10-DOCUMENT`; it also exposes planned token `XP-XSD10-FULL`, which generator-core
  rejects until the full XSD 1.0 implementation gates accept support.

The API does not expose parser, IR, binding, or emitter implementation types.

## Usage shape

```java
Generator generator = new io.github.mundanej.mxjb.generator.core.CoreGenerator();
GeneratorRequest request = GeneratorRequest.of(
    java.util.List.of(java.nio.file.Path.of(
        "examples/purchase-order/src/main/resources/schema/purchase-order.xsd")),
    java.nio.file.Path.of("build/generated/mxjb-api"));
GeneratorResult result = generator.generate(request);
```

## Contributor notes

- Do not expose implementation parser or compiler internals.
- Keep API additions tied to requirement IDs and ADR review.
- Add package documentation for every public package.
