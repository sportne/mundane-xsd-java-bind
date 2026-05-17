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
- Scalar lexical conversion covers `String`, `Boolean`, `Integer`, `BigInteger`, `Long`, and `BigDecimal`; accepted `XP-VALIDATION-10-BASIC` facets are emitted as explicit generated validator checks after lexical conversion.
- Generated reader source uses fully qualified names for generated model types, `java.util.Objects`, collection helpers, and `runtime-core` `XmlName`, `XmlEventReader`, `XmlDiagnostic`, `XmlDiagnosticSeverity`, and `XmlReadException` to avoid import collisions.
- Generated reader source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML writer behavior, validation engine behavior, or external resource access.

## `TASK-0016` validator source shape

The first generated-validator emitter supports basic validation for supported data-structure record models.

- One public final validator class is emitted per root element in `<model-package>.xml`, named `<RootTypeSimpleName>XmlValidator`.
- Validator classes expose `validate(RootType value)` for object validation and `validate(XmlEventReader input)` for location-aware XML validation through the generated peer reader.
- Object validation checks required singleton values, repeated `minOccurs`, finite repeated `maxOccurs`, and nested model values in deterministic binding order; object diagnostics use `XmlLocation.UNKNOWN`.
- XML validation preserves generated-reader diagnostics by converting `XmlReadException` diagnostics into `ValidationError` values before returning `ValidationResult.invalid(...)`.
- Generated validator source uses fully qualified names for generated model types and `runtime-core` validation/XML types to avoid import collisions.
- Generated validator source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML writer behavior, dependency injection, or external resource access.

## `TASK-0023` choice model shape

The `0.2.0` Practical Data Contracts implementation accepts a narrow generated model shape for
opt-in profile `XP-DATA-10-CHOICE`.

- Each accepted choice particle becomes one generated model field.
- Required choices use `<ContainingTypeSimpleName>Choice`; optional choices use `Optional<<ContainingTypeSimpleName>Choice>`.
- The choice field type is a generated sealed interface named `<ContainingTypeSimpleName>Choice`.
- Each branch is a generated record named `<BranchElementSimpleName>Choice` and carries the existing scalar or generated model value for that branch.
- Generated readers and writers dispatch on the sealed choice type explicitly and must keep the generated-code ban on reflection, annotations, ServiceLoader, dynamic proxies, parser APIs, and external resource access.

This shape does not imply support for repeated choices, nested model groups, wildcards,
substitution groups, mixed content, anonymous branch complex types, or full XSD 1.0 model groups.

## `TASK-0027` composed-schema shape

The `XP-XSD10-COMPOSED` profile preserves the generated-code contract instead of introducing new
runtime binding mechanisms.

- `TASK-0027` model groups and attribute groups are flattened before emission, so generated records
  keep ordinary field and attribute components in deterministic schema order.
- Planned complex-type extension is flattened with base fields before derived fields; generated
  Java model classes do not use inheritance for the planned derivation subset.
- Planned list-valued simple types use immutable `List<T>` record components and explicit generated
  reader/writer/validator token handling.
- Planned union-valued simple types use lexical `String` record components with explicit generated
  validator checks for member alternatives.
- Generated source must keep the existing bans on annotations for binding, reflection, ServiceLoader,
  dynamic proxies, parser APIs in generated code, third-party runtime dependencies, and external
  resource access.

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
