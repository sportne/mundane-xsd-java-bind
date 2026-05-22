# Runtime architecture

The runtime is intentionally small.

## `runtime-core`

Active public concepts:

- `XmlName`: namespace URI + local name.
- `XmlAttribute`: retained fragment attribute with expanded name and lexical value.
- `XmlFragment`, `XmlFragmentContent`, `XmlFragmentText`, and `XmlFragmentElement`: immutable
  retained unknown XML element fragments for accepted wildcard/open-content fields.
- `XmlDuration`, `XmlDateTime`, `XmlDate`, `XmlTime`, `XmlGYear`, `XmlGYearMonth`, `XmlGMonth`,
  `XmlGMonthDay`, `XmlGDay`, `XmlBinary`, `XmlAnyUri`, and `XmlQName`: exact XML Schema datatype
  values used where Java standard types do not preserve project semantics.
- `XmlDatatypes`: dependency-free XML Schema lexical conversion, list conversion, formatting, and
  facet helper engine for generated code.
- `XmlLocation`: line/column/system ID where available.
- `XmlDiagnostic`: structured error/warning data with stable code, message, severity, and location.
- `XmlReadException` and `XmlWriteException`: checked exceptions that retain an `XmlDiagnostic`.
- `XmlEventReader`: project-owned pull/event abstraction.
- `XmlOutput`: project-owned writer abstraction.
- `ValidationError` and `ValidationResult`.

`runtime-core` remains dependency-free and parser-neutral. It defines generated-code-facing values,
datatype conversion helpers, and interfaces only; it does not parse XML documents, adapt JDK XML
APIs, generate code, or validate schema documents by itself.

Generated writer source emitted by `TASK-0012` targets `XmlOutput` and `XmlName` directly. Concrete XML serialization, namespace prefix assignment, and adapter behavior remain outside `runtime-core`; generated writers operate only on expanded names and scalar text values.

Generated reader source emitted by `TASK-0015` targets `XmlEventReader`, `XmlName`, `XmlLocation`, `XmlDiagnostic`, and `XmlReadException` directly. Concrete XML parsing, entity/resource policy, and adapter behavior remain outside generated code; generated readers consume only the project-owned event abstraction.

Generated validator source emitted by `TASK-0016` targets `ValidationResult`, `ValidationError`, `XmlLocation`, `XmlEventReader`, and generated peer readers directly. `runtime-core` supplies validation value objects only; generated validators contain the explicit schema-specific validation logic.

## `runtime-jdkxml`

Optional adapter module that bridges JDK StAX to `runtime-core` interfaces. Generated code must not require this module; it is a convenience adapter for tests, examples, and user code.

Active public concepts:

- `JdkXmlAdapters.secureInputFactory()`: creates a JDK StAX input factory with DTD and external entity support disabled and external XML resources denied by default.
- `JdkXmlAdapters.eventReader(XMLStreamReader)`: adapts a StAX reader to `XmlEventReader`.
- `JdkXmlAdapters.output(XMLStreamWriter)`: adapts a StAX writer to `XmlOutput`.

The adapter maps StAX element, text, document, and end events to the project event model and preserves best-effort StAX location data. Writer adapters assign deterministic namespace prefixes for non-empty namespace URIs. Adapter diagnostics use stable `MXJB-JDKXML-*` codes and wrap the original JDK XML exception.

## `TASK-0037` open-content runtime shape

`TASK-0037` adds retained-fragment values for `XP-XSD10-DOCUMENT` wildcard fields. The values are
dependency-free and parser-neutral. They are immutable, use defensive copies, preserve null-free
invariants, and remain safe for Native Image.

Retained fragments preserve expanded element names, attributes, text content, and nested retained
element fragments. They do not wrap DOM, StAX, SAX, XPath, parser handles, mutable maps, namespace
prefix state, comments, processing instructions, entity references, or external resources.

## `TASK-0038` mixed-content runtime shape

`TASK-0038` adds no new `runtime-core` public values. Mixed-content model types are generated per
containing schema type as sealed content-list interfaces and branch records. Accepted wildcard
branches reuse `XmlFragment`; text and known-element branches are ordinary generated values.

## `TASK-0039` serialization policy runtime shape

`TASK-0039` adds no new `runtime-core` public values. Stable project serialization is defined by
the existing `XmlOutput` abstraction plus generated writer ordering rules.

Generated writers emit schema-owned attributes immediately after `startElement`, then child content
in binding order, repeated values in list order, and mixed-content branches in content-list order.
Retained `XmlFragment` values are emitted through `XmlOutput` using `XmlFragment.attributes()` and
`XmlFragment.content()` list order. This is deterministic project output, not W3C XML
Canonicalization.

`runtime-jdkxml` remains the optional concrete adapter for tests and examples. Its writer adapter
assigns deterministic generated prefixes such as `ns1` and `ns2` for namespaces that do not already
have a prefix on the wrapped `XMLStreamWriter`; it does not preserve lexical prefixes from input XML.
Text and attribute escaping are delegated to the wrapped JDK XML writer and verified by reparsing the
serialized XML through the secure adapter.
