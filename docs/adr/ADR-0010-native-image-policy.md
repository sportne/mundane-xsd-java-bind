# ADR-0010: Native Image policy

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Native Image compatibility is a first-class acceptance criterion with explicit smoke and conformance stages.

## Consequences

Avoids late discovery of reflection/resource/proxy issues.

## Requirements

`REQ-NI-001`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
