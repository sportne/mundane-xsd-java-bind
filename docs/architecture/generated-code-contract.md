# Generated-code contract

Generated source must be production-quality Java.

## Rules

1. Generated model types are immutable by default.
2. Use Java records for simple data-structure complex types where appropriate.
3. Use final classes/builders when records are unsuitable.
4. Use sealed interfaces or tagged types for choices where feasible.
5. Use defensive copies for repeated elements.
6. Maintain null-free collection invariants.
7. Do not use runtime annotations for binding behavior.
8. Do not use reflection for construction, property access, or codec dispatch.
9. Do not rely on classpath scanning or ServiceLoader in generated runtime paths.
10. Generated source must pass formatting and static analysis.

## `TASK-0011` model source shape

The first generated-model emitter supports data-structure record candidates only.

- Required fields are non-null reference types and compact constructors call `Objects.requireNonNull`.
- Optional fields are `Optional<T>` values and the `Optional` reference itself must be non-null.
- Repeated fields are `List<T>` values and compact constructors assign `List.copyOf(Objects.requireNonNull(...))`.
- Supported scalar mappings are `xs:string` to `String`, `xs:boolean` to `Boolean`, `xs:int` to `Integer`, `xs:integer` to `BigInteger`, `xs:long` to `Long`, and `xs:decimal` to `BigDecimal`.
- Generated model source contains no binding annotations, XML reader/writer behavior, validation methods, reflection, ServiceLoader, or classpath scanning.

## Package layout example

```text
com.example.invoice
  Invoice.java
  LineItem.java
  Address.java
  InvoiceChoice.java

com.example.invoice.xml
  InvoiceXml.java
  InvoiceXmlReader.java
  InvoiceXmlWriter.java
  InvoiceXmlValidator.java
  InvoiceSchema.java

com.example.invoice.xml.internal
  InvoiceNames.java
  InvoiceParsers.java
  InvoiceWriters.java
```
