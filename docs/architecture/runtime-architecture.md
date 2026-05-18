# Runtime architecture

The runtime is intentionally small.

## `runtime-core`

Active public concepts:

- `XmlName`: namespace URI + local name.
- `XmlLocation`: line/column/system ID where available.
- `XmlDiagnostic`: structured error/warning data with stable code, message, severity, and location.
- `XmlReadException` and `XmlWriteException`: checked exceptions that retain an `XmlDiagnostic`.
- `XmlEventReader`: project-owned pull/event abstraction.
- `XmlOutput`: project-owned writer abstraction.
- `ValidationError` and `ValidationResult`.

`runtime-core` remains dependency-free and parser-neutral. It defines generated-code-facing values and interfaces only; it does not parse XML, adapt JDK XML APIs, generate code, or validate documents by itself.

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

## `TASK-0036` planned open-content runtime shape

`TASK-0036` plans a narrow `runtime-core` extension for `TASK-0037`; it does not add the types yet.
The planned public values are dependency-free and parser-neutral:

- `XmlAttribute`: expanded attribute name plus lexical value.
- `XmlNamespaceDeclaration`: prefix plus namespace URI for retained fragment serialization.
- `XmlFragment`: retained unknown element subtree with expanded name, immutable attributes,
  immutable namespace declarations, and immutable ordered content.
- `XmlFragmentContent`: sealed content value with text and nested element-fragment branches.

These values must be immutable, use defensive copies, preserve null-free invariants, and remain
safe for Native Image. They must not wrap DOM, StAX, SAX, XPath, parser handles, mutable maps, or
external resources.
