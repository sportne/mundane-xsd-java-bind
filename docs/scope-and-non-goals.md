# Scope and non-goals

## In scope

- XML Schema-to-Java generation.
- Generated model types, XML writers, XML readers, validators, and metadata.
- XSD 1.0 data-structure subset as the first implementation profile.
- Validation-ready architecture from day one.
- Optional tool/test use of JDK XML APIs.
- CLI and Gradle plugin generation entry points.
- Native Image smoke and conformance testing.

## Out of scope

- Code-to-schema generation.
- JAXB API compatibility as a product requirement.
- Annotation-driven runtime behavior.
- Runtime reflection-based binding.
- WSDL/SOAP ecosystem generation.
- XSLT, XQuery, or Schematron engines.
- DOM-first generated runtime.
- DTD-to-code generation.

## Scope-control rule

Any expansion of standards, schema constructs, runtime dependencies, or validation semantics requires requirement updates, conformance matrix updates, and ADR review.
