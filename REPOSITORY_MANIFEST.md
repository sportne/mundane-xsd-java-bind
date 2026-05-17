# Repository manifest

This repository contains the design-control baseline and the implemented first
public vertical slice for `mundane XSD Java Binding`.

The current tree includes:

- root design-control pack manifest
- agent governance
- Gradle multi-project build
- build convention plugins
- configuration files for Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, and JaCoCo
- documentation baseline
- ADR baseline
- requirements taxonomy
- verification plan
- infrastructure plan
- CI workflows
- runtime, generator, CLI, Gradle plugin, and example modules
- first-slice XML schema-to-Java implementation for the `XP-DATA-10` profile

Implemented product capabilities include schema resolution, parsing, IR and
binding construction, generated model/writer/reader/basic-validator emission,
runtime XML abstractions, an optional JDK StAX adapter, public generator API,
CLI, Gradle plugin, examples, and representative JVM/Native Image verification.

Deferred capabilities remain documented in the roadmap and conformance matrix,
including `xs:choice`, practical simple-type facets, derivation, open content,
mixed content, identity constraints, and XSD 1.1.
