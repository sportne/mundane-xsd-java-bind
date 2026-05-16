# ADR-0006: XSD 1.0 first, XSD 1.1 future

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Implement the XSD 1.0 data-structure profile first. XSD 1.1 features are future-profile gated.

## Consequences

Allows a useful vertical slice without conflating assertions and advanced 1.1 semantics.

## Requirements

`REQ-STD-*`, `REQ-SCHEMA-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
