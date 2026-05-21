# ADR-0006: XSD 1.0 profile gates

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Implement XSD 1.0 through explicit compatibility profiles, starting with practical data-structure
binding and widening only through accepted task gates. XSD 1.1 is not a project target.

## Consequences

Allows a useful vertical slice without conflating assertions and advanced 1.1 semantics.

## Requirements

`REQ-STD-*`, `REQ-SCHEMA-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
