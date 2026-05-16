# ADR-0012: Offline build policy

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Support offline builds using a hydrated local Maven repository, dependency verification metadata, dependency locks, committed wrapper metadata, and a provisioned Gradle distribution.

## Consequences

Supports reproducibility and controlled enterprise environments.

## Requirements

`REQ-BUILD-*`, `REQ-SEC-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
