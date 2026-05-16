# ADR-0004: Generated code uses no runtime reflection

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Generated readers, writers, validators, and model construction must use explicit code rather than reflection-based mappers.

## Consequences

Improves static analyzability, debuggability, performance predictability, and Native Image behavior.

## Requirements

`REQ-RT-002`, `REQ-NI-001`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
