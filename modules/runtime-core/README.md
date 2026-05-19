# runtime-core

Runtime core API and primitives; no third-party dependencies.

## Current status

`TASK-0010` runtime primitives are implemented. This module exposes dependency-free values and interfaces for generated bindings:

- XML names, locations, diagnostics, and checked read/write exceptions.
- Pull-style XML event reader and output interfaces.
- Dependency-free retained-fragment values for accepted open content: `XmlFragment`,
  `XmlFragmentContent`, `XmlFragmentText`, `XmlFragmentElement`, and `XmlAttribute`.
- Validation error and result values.

No parser adapter, XML reader/writer implementation, validation engine, generated source, CLI behavior, or Gradle plugin behavior belongs in this module.

Retained fragments are parser-neutral values used by generated `XP-XSD10-DOCUMENT` bindings for
accepted wildcard content. They preserve expanded names, attributes, text, and nested element
fragments without DOM, parser handles, external resource access, comments/PI retention, or
entity-reference identity.

## Contributor notes

- Keep generated-code runtime paths dependency-free.
- Keep `runtime-core` parser-neutral; JDK XML adapters belong in `runtime-jdkxml`.
- Keep public runtime concepts documented before generated code depends on them.
