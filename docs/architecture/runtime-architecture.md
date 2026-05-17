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

## `runtime-jdkxml`

Optional adapter module that may bridge JDK StAX/JAXP to `runtime-core` interfaces. Generated code must not require this module; it is a convenience adapter.
