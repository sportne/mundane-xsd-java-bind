# runtime-jdkxml

Optional adapters for JDK XML APIs.

## Current status

Scaffold only. This module may adapt JDK `java.xml` types to `runtime-core` abstractions, but generated code must not require it.

## Contributor notes

- Do not make this module a required generated-code dependency.
- Keep parser/resource-resolution behavior aligned with the security architecture.
- Add package documentation for every public package.
