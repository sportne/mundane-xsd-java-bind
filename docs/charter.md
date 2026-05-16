# Project charter

## Mission

`mundane XSD Java Binding` shall provide a modern Java XML Schema-to-code generator and runtime that emits explicit Java model, XML writer, XML reader, and validation code from XML Schema documents.

## Operating principles

1. Treat XML Schema as a source language and Java binding as compiled output.
2. Favor explicit generated code over reflective runtime behavior.
3. Preserve XML namespace and schema semantics in the binding model.
4. Keep runtime core and generated code dependency-free except for the JDK.
5. Make Generated Java readable, deterministic, testable, and statically analyzable.
6. Design for GraalVM Native Image from the beginning.
7. Separate standards conformance, compatibility profiles, requirements, architecture, and implementation.

## First success milestone

The first public vertical slice is successful when a user can run a CLI or Gradle task against an XSD data-structure schema and receive generated Java model, reader, writer, and basic validation code that compiles under Java 21, round-trips representative XML, and runs under Native Image smoke tests.
