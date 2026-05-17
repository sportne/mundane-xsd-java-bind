# purchase-order

Representative `XP-DATA-10` purchase-order fixture.

This example keeps generated-style model, reader, writer, and validator sources checked in as
approved fixture output until the public CLI and Gradle plugin are implemented. The generated
sources depend only on `runtime-core`; tests use the optional `runtime-jdkxml` adapter to feed and
write JDK StAX events.

## Contents

- `src/main/resources/schema/purchase-order.xsd`: single-namespace purchase-order schema.
- `src/main/java/com/example/purchase`: generated-style model sources.
- `src/main/java/com/example/purchase/xml`: generated-style reader, writer, and validator.
- `src/test/resources/xml/purchase-order.xml`: canonical XML fixture.
- `PurchaseOrderRoundTripTest`: XML to object to XML round-trip, object validation, XML validation,
  and negative reader-diagnostic preservation.

Run:

```bash
./gradlew :examples:purchase-order:check
```
