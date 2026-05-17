# multi-namespace

Representative `XP-DATA-10` multi-namespace fixture.

This example keeps generated-style model, reader, writer, and validator sources checked in as
approved fixture output for the repository's multi-project quality gate. The same namespace/import
shape is covered by the public CLI and Gradle plugin generation tests, including equivalent sources
generated through `generateMxjbSources`. It exercises an `urn:orders` root schema that imports an
`urn:lines` line-item schema. The generated sources depend only on `runtime-core`; tests use the
optional `runtime-jdkxml` adapter. The example's tests are part of the root `nativeSmoke`
aggregate.

## Contents

- `src/main/resources/schema/order.xsd`: order schema with `xs:import`.
- `src/main/resources/schema/line.xsd`: imported line-item schema.
- `src/main/java/com/example/orders` and `src/main/java/com/example/lines`: generated-style models.
- `src/main/java/com/example/orders/xml`: generated-style reader, writer, and validator.
- `src/test/resources/xml/order.xml`: canonical XML fixture.
- `MultiNamespaceRoundTripTest`: namespace-aware XML round-trip, object validation, XML validation,
  and namespace-mismatch diagnostic preservation.

Run:

```bash
./gradlew :examples:multi-namespace:check
```

With GraalVM native-image available, the representative native lane includes:

```bash
./gradlew :examples:multi-namespace:nativeTest
```
