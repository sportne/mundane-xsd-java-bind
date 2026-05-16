# ADR-0007: Validation phasing

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Model validation architecture from day one, but phase implementation by constraint class.

## Consequences

Prevents reader/writer design from becoming validation-hostile while controlling first-slice scope.

## Requirements

`REQ-VAL-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
