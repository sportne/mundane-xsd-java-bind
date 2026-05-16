# ADR-0003: Runtime dependency boundary

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Runtime core and generated code must not require third-party dependencies. Generator, tests, CLI, Gradle plugin, and tooling may use dependencies.

## Consequences

Preserves lightweight generated artifacts and Native Image friendliness.

## Requirements

`REQ-RT-001`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
