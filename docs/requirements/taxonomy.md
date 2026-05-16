# Requirements taxonomy

## Requirement record format

```text
ID:
Title:
Category:
Profile(s):
Phase:
Statement:
Rationale:
Verification:
Status:
Trace:
```

## Status values

```text
proposed | accepted | implemented | verified | deferred | rejected | superseded
```

## Trace fields

- ADR IDs
- architecture docs
- module(s)
- test class or test suite
- conformance matrix row
- issue or task card

## Review rule

A requirement may not move to `implemented` unless there is a test strategy. A requirement may not move to `verified` unless there is an automated or documented manual verification record.
