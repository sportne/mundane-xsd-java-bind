# Runtime architecture

The runtime is intentionally small.

## `runtime-core`

Planned public concepts:

- `XmlName`: namespace URI + local name.
- `XmlLocation`: line/column/system ID where available.
- `XmlDiagnostic`: structured error/warning data.
- `XmlReadException` and `XmlWriteException`.
- `XmlEventReader`: project-owned pull/event abstraction.
- `XmlOutput`: project-owned writer abstraction.
- `ValidationError` and `ValidationResult`.

No product implementation exists in this pack.

## `runtime-jdkxml`

Optional adapter module that may bridge JDK StAX/JAXP to `runtime-core` interfaces. Generated code must not require this module; it is a convenience adapter.
