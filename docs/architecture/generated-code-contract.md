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

## `TASK-0012` writer source shape

The first generated-writer emitter supports root static XML writers for supported data-structure record models.

- One public final writer class is emitted per root element in `<model-package>.xml`, named `<RootTypeSimpleName>XmlWriter`.
- The public writer entry point is `public static void write(XmlOutput output, RootType value) throws XmlWriteException`.
- Writer classes have private constructors, static `XmlName` constants, and private static helper methods for nested complex types.
- Writers require non-null `output` and root `value`, write attributes immediately after `startElement`, write child elements in binding order, skip empty `Optional` values, and iterate repeated fields in list order.
- Scalar lexical conversion uses the string value directly for `String` and `String.valueOf(...)` for non-string scalar values.
- Generated writer source uses fully qualified names for generated model types, `java.util.Objects`, and `runtime-core` `XmlName`, `XmlOutput`, and `XmlWriteException` to avoid import collisions with schema-derived model names.
- Generated writer source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML reader behavior, validation behavior, or external resource access.

## `TASK-0015` reader source shape

The first generated-reader emitter supports root static XML readers for supported data-structure record models.

- One public final reader class is emitted per root element in `<model-package>.xml`, named `<RootTypeSimpleName>XmlReader`.
- The public reader entry point is `public static RootType read(XmlEventReader input) throws XmlReadException`.
- Reader classes have private constructors, static `XmlName` constants, and private static helper methods for nested complex types.
- Readers require non-null `input`, accept `START_DOCUMENT` or root `START_ELEMENT`, skip whitespace-only text between elements, match expanded names namespace-aware, and parse children in binding order.
- Optional fields are produced as `Optional.empty()` when absent, repeated fields are copied into immutable lists through generated model construction, and missing/repeated/out-of-order content produces deterministic `XmlReadException` diagnostics.
- Scalar lexical conversion covers `String`, `Boolean`, `Integer`, `BigInteger`, `Long`, and `BigDecimal`; full XSD datatype/facet validation remains a later validation task.
- Generated reader source uses fully qualified names for generated model types, `java.util.Objects`, collection helpers, and `runtime-core` `XmlName`, `XmlEventReader`, `XmlDiagnostic`, `XmlDiagnosticSeverity`, and `XmlReadException` to avoid import collisions.
- Generated reader source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML writer behavior, validation engine behavior, or external resource access.

## Package layout example

```text
com.example.invoice
  Invoice.java
  LineItem.java
  Address.java
  InvoiceChoice.java

com.example.invoice.xml
  InvoiceXmlWriter.java
  InvoiceXmlReader.java
  InvoiceXmlValidator.java
  InvoiceSchema.java

com.example.invoice.xml.internal
  InvoiceNames.java
  InvoiceParsers.java
  InvoiceWriters.java
```
