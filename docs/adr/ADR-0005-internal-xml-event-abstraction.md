# ADR-0005: Internal XML event abstraction

Status: accepted
Date: 2026-05-16

## Context

`mundane XSD Java Binding` is a design-first XML Schema-to-Java generator and runtime project. The repository must establish stable architectural decisions before product implementation.

## Decision

Generated codecs target project-owned XML event/output interfaces. JDK XML APIs are optional adapters, not generated-code dependencies.

## Consequences

Avoids locking generated code to StAX/DOM/SAX and enables controlled runtime semantics.

## Requirements

`REQ-RT-*`, `REQ-XML-R-*`, `REQ-XML-W-*`

## Verification

The decision is verified through requirements traceability, architecture tests where applicable, build configuration, conformance matrix updates, and coding-agent task review.
