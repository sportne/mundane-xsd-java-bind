# ADR-0001: Standards baseline

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Use XML 1.0, Namespaces in XML 1.0, XML Schema 1.0 Structures/Datatypes, XML Infoset, XML Base, and xml:id as the primary design baseline. XML 1.1 and XSD 1.1 remain future profiles.

## Consequences

Provides a stable standards foundation and prevents accidental expansion of scope.

## Requirements

`REQ-STD-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
