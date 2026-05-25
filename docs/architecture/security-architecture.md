# Security architecture

## XML security posture

- Network access is denied by default during schema resolution and XML reading.
- External entity resolution must be disabled or explicitly controlled by resolver policy.
- The default JDK XML adapter factory disables DTD and external entity support and installs a resolver that denies external XML resources.
- Conformance harness XML parsers and schema validators must use the same denial posture when they
  parse local suite metadata or compare generated bindings with JDK XML Schema validation.
- Recursive includes/imports must have cycle detection.
- Resource size, nesting depth, and entity expansion risks must be bounded.
- Diagnostics must not leak local secrets or full environment paths except in explicit debug mode.

## Resolver policy

The resolver shall support:

- local file resolution
- explicit schema catalog mapping
- offline repository/resource roots
- denied network by default
- opt-in integration tests for remote resources only when tagged

## Agent rule

Any change that weakens resolver restrictions requires `ADR-0014` update or replacement.
