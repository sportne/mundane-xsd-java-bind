# ADR-0008: Generated model style

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Generated model types are immutable Java 21 types, using records, final classes/builders, and sealed types as appropriate.

## Consequences

Matches modern Java idioms and static-analysis goals.

## Requirements

`REQ-MODEL-*`, `REQ-GEN-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
