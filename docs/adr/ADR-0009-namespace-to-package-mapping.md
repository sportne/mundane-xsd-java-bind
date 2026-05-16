# ADR-0009: Namespace-to-package mapping

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Namespace-to-package mapping must be deterministic and externally configurable.

## Consequences

Prevents unstable generated APIs and supports multi-namespace schemas.

## Requirements

`REQ-NS-001`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
