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

## Unsupported feature behavior

Unsupported schema features must produce explicit diagnostics. Silent partial interpretation is forbidden.
