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
- Scalar lexical conversion uses `XmlDatatypes.format(...)` and `XmlDatatypes.formatList(...)`
  for supported XML Schema datatype values; QName output delegates prefix assignment to
  `XmlOutput.qNameText(...)`.
- Generated writer source uses fully qualified names for generated model types, `java.util.Objects`, and `runtime-core` `XmlName`, `XmlOutput`, and `XmlWriteException` to avoid import collisions with schema-derived model names.
- Generated writer source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML reader behavior, validation behavior, or external resource access.

## `TASK-0015` reader source shape

The first generated-reader emitter supports root static XML readers for supported data-structure record models.

- One public final reader class is emitted per root element in `<model-package>.xml`, named `<RootTypeSimpleName>XmlReader`.
- The public reader entry point is `public static RootType read(XmlEventReader input) throws XmlReadException`.
- Reader classes have private constructors, static `XmlName` constants, and private static helper methods for nested complex types.
- Readers require non-null `input`, accept `START_DOCUMENT` or root `START_ELEMENT`, skip whitespace-only text between elements, match expanded names namespace-aware, and parse children in binding order.
- Optional fields are produced as `Optional.empty()` when absent, repeated fields are copied into immutable lists through generated model construction, and missing/repeated/out-of-order content produces deterministic `XmlReadException` diagnostics.
- Scalar lexical conversion uses the shared `XmlDatatypes` runtime engine for all supported XML
  Schema 1.0 built-ins in accepted schema shapes. Generated QName element readers parse while the
  element namespace context is still active; list-valued built-ins and named lists are copied into
  immutable lists.
- Generated reader source uses fully qualified names for generated model types, `java.util.Objects`, collection helpers, and `runtime-core` `XmlName`, `XmlEventReader`, `XmlDiagnostic`, `XmlDiagnosticSeverity`, and `XmlReadException` to avoid import collisions.
- Generated reader source contains no annotations, reflection, ServiceLoader, classpath scanning, XML parser APIs, XML writer behavior, validation engine behavior, or external resource access.

## `TASK-0016` validator source shape

The first generated-validator emitter supports basic validation for supported data-structure record models.

- One public final validator class is emitted per root element in `<model-package>.xml`, named `<RootTypeSimpleName>XmlValidator`.
- Validator classes expose `validate(RootType value)` for object validation and `validate(XmlEventReader input)` for location-aware XML validation through the generated peer reader.
- Object validation checks required singleton values, repeated `minOccurs`, finite repeated `maxOccurs`, and nested model values in deterministic binding order; object diagnostics use `XmlLocation.UNKNOWN`.
- XML validation preserves generated-reader diagnostics by converting `XmlReadException` diagnostics into `ValidationError` values before returning `ValidationResult.invalid(...)`.
- Datatype and facet validation delegates to the shared runtime datatype engine while preserving
  stable generated diagnostic categories for length, range, pattern, and general datatype facet
  failures.
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

`TASK-0051` extends the same generated shape to repeated element-only choices by using
`List<<ContainingTypeSimpleName>Choice>` fields whose list order controls writer order and validator
iteration. This shape still does not imply support for wildcard choice branches, mixed choice
branches, anonymous branch complex types, or derivation-polymorphic choices.

## `TASK-0027` through `TASK-0029` composed-schema shape

The `XP-XSD10-COMPOSED` profile preserves the generated-code contract instead of introducing new
runtime binding mechanisms.

- `TASK-0027` model groups and attribute groups are flattened before emission, so generated records
  keep ordinary field and attribute components in deterministic schema order.
- `TASK-0051` required `xs:all` groups and optional all-groups whose members are all optional flatten
  into ordinary fields with unordered generated reader acceptance and deterministic writer output in
  binding order; non-flattenable repeated/optional multi-particle groups remain deterministic
  diagnostics.
- `TASK-0029` complex-type extension is flattened with base fields before derived fields; generated
  Java model classes do not use inheritance for the accepted derivation subset.
- `TASK-0028` list-valued simple types use immutable `List<T>` record components for required
  singleton XML values and explicit generated reader/writer/validator token handling.
- `TASK-0028` union-valued simple types use lexical `String` record components with explicit
  generated validator checks for member alternatives.
- Generated source must keep the existing bans on annotations for binding, reflection, ServiceLoader,
  dynamic proxies, parser APIs in generated code, third-party runtime dependencies, and external
  resource access.

## `TASK-0032` semantic model shape

The `XP-XSD10-SEMANTIC` profile continues the explicit generated-code contract.

- `TASK-0032` required singleton nillable elements use `Optional<T>` record components, where
  `Optional.empty()` represents explicit `xsi:nil` rather than an absent optional element.
- `TASK-0032` defaulted and fixed scalar attributes appear as effective scalar field values after
  generated reader construction; present empty simple elements may use element defaults, and
  generated validators check fixed values explicitly.
- `TASK-0032` generated writers emit explicit `xsi:nil="true"` for nillable empty values and
  serialize effective scalar attribute values without tracking whether they came from XML input or
  schema defaults.
- `TASK-0033` substitution group support uses a generated sealed interface plus one record branch
  per accepted concrete head or member element, preserving the actual XML element name for
  generated reader and writer dispatch.
- Generated source must keep existing bans on binding annotations, reflection, ServiceLoader,
  dynamic proxies, parser APIs in generated code, third-party runtime dependencies, and external
  resource access.

## `TASK-0037` document wildcard model shape

The `XP-XSD10-DOCUMENT` profile keeps document-oriented behavior explicit and generated.

- `TASK-0037` adds the public profile token and supports accepted direct `xs:any` particles as
  immutable `List<XmlFragment>` fields. `XmlFragment` is a dependency-free `runtime-core` value,
  not a DOM node or parser-specific object.
- `TASK-0037` retained fragments preserve expanded element names, attributes, text children, and
  nested retained elements. Exact lexical prefixes, comments, processing instructions, entity
  references, DTDs, and external resources stay out of scope.
- Generated readers capture accepted wildcard element subtrees into `XmlFragment` values; generated
  writers emit retained fragments through `XmlOutput`; generated validators check wildcard
  cardinality, null-free fragment structure, and namespace constraints.
- `TASK-0038` mixed content uses a generated sealed interface named
  `<ContainingTypeSimpleName>Content`. Branch records represent text, accepted known elements, and
  accepted wildcard fragments in source order; generated records expose an immutable
  `List<<ContainingTypeSimpleName>Content>`.
- Generated mixed-content readers preserve non-whitespace text nodes and drop whitespace-only text
  nodes deterministically. Generated writers serialize the content list exactly in list order.
- `TASK-0039` verifies stable project XML output for generated and retained content. Generated
  writers emit attributes immediately after `startElement`, ordinary child content in binding order,
  repeated content in list order, and mixed-content branches in content-list order. Retained
  wildcard fragments emit `XmlFragment.attributes()` and `XmlFragment.content()` in stored list
  order. Namespace prefix assignment and escaping are adapter responsibilities. The project does
  not claim W3C XML Canonicalization or cryptographic canonical XML compatibility.
- `TASK-0052` adds accepted `xs:anyAttribute` binding as a collision-safe immutable
  `List<XmlAttribute>` field, normally named `wildcardAttributes`. Generated readers reject
  prohibited declared attributes before wildcard capture, capture matching wildcard attributes in
  source order, and keep rejecting unexpected attributes outside the effective wildcard namespace
  constraint. Generated writers emit declared attributes first and wildcard attributes in list
  order. Generated validators check non-null wildcard lists/items, namespace constraints, and
  prohibited/excluded attribute names.
- `TASK-0052` carries `processContents` metadata for retained element and attribute wildcards and
  defaults omitted values to XSD 1.0 `strict`. Full schema-known deep validation for `lax`/`strict`
  wildcards and wildcard choice branch generation remain future tasks.
- Generated source must keep existing bans on binding annotations, reflection, ServiceLoader,
  dynamic proxies, parser APIs in generated code, third-party runtime dependencies, DOM-backed
  binding, and external resource access.

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
