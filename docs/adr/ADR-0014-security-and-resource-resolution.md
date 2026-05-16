# ADR-0014: Security and resource resolution

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Schema and XML resource resolution is deny-by-default for network access and must use explicit resolver policy.

## Consequences

Reduces XXE, SSRF, and uncontrolled dependency risks.

## Requirements

`REQ-SEC-*`, `REQ-RES-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
