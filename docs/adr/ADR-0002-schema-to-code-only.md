# ADR-0002: Schema-to-code only

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

The project generates Java from XML Schema only. It shall not generate XML Schema from Java code.

## Consequences

Keeps the compiler direction clear and avoids JAXB-style bidirectional complexity.

## Requirements

`REQ-SCOPE-001`, `REQ-SCOPE-002`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
