# Conformance strategy

## Goals

- Avoid claiming broad XML Schema conformance before it exists.
- Map every supported feature to a profile and test set.
- Use W3C XML and XML Schema suites as reference material where practical.
- Separate supported, unsupported-by-design, future, and blocked statuses.

## Conformance status values

```text
not-started | designed | implemented | partially-supported | unsupported-by-design | future | blocked
```

## Test selection rule

A W3C or external conformance test is included only when:

- it maps to a declared profile, or
- it is used to prove unsupported-feature diagnostics, or
- it is part of a future-profile readiness study.

## `0.6.0` hardening intake plan

`TASK-0041` defines the planning scope for hardening conformance and interop evidence without
claiming full XML Schema conformance. `TASK-0042` is the first implementation gate for this lane.

Selected W3C or external fixtures must be classified before they are executable evidence:

- `supported-profile`: the fixture maps to one declared compatibility profile and expected
  behavior is already implemented.
- `unsupported-diagnostic`: the fixture proves deterministic rejection for an unsupported schema or
  XML construct.
- `future-study`: the fixture informs a future profile but does not affect current support claims.
- `blocked`: the fixture cannot run locally because of licensing, toolchain, storage, or dependency
  constraints; the blocker must be documented.

Interop comparisons must be repeatable from local inputs. Accepted comparison targets are JDK XML
Schema validation, secure JDK XML parsing, generated binding round trips, and documented semantic
comparisons. Byte-identical XML output, W3C XML Canonicalization, XML Signature canonical forms,
networked test retrieval, and broad vendoring of external suites remain out of scope unless a later
task and ADR approve them.

## Unsupported feature behavior

Unsupported schema features must produce explicit diagnostics. Silent partial interpretation is forbidden.
