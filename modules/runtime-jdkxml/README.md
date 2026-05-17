# runtime-jdkxml

Optional adapters for JDK XML APIs.

## Current status

This module adapts JDK StAX `java.xml` types to `runtime-core` abstractions, but generated code must not require it.

Public entry points:

- `JdkXmlAdapters.secureInputFactory()` creates an `XMLInputFactory` with DTD and external entity support disabled and a resolver that denies external XML resources by default.
- `JdkXmlAdapters.eventReader(XMLStreamReader)` adapts StAX input to `XmlEventReader`.
- `JdkXmlAdapters.output(XMLStreamWriter)` adapts StAX output to `XmlOutput`.

## Contributor notes

- Do not make this module a required generated-code dependency.
- Keep parser/resource-resolution behavior aligned with the security architecture.
- Add package documentation for every public package.
