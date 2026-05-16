# ADR-0011: Build quality gates

Status: accepted
Date: 2026-05-16

## Context

`xsd-bind-java` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Gradle quality gates are mandatory and may not be weakened casually.

## Consequences

Maintains engineering discipline and protects agent-driven changes.

## Requirements

`REQ-QA-001`, `REQ-BUILD-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
