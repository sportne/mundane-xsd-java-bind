# conformance-tests

Internal conformance test harness; not published.

## Current status

`TASK-0017` adds selected local `XP-DATA-10` fixtures that exercise the implemented phase-one
behavior without claiming full W3C XML Schema conformance. The tests reuse the checked-in generated
fixture sources from the purchase-order and multi-namespace examples.

Covered behavior:

- XML to object to XML round trips for representative generated bindings.
- Namespace-aware element matching across imported-schema fixture boundaries.
- Required-content, out-of-order sequence, namespace mismatch, and scalar lexical diagnostics.

## Contributor notes

- Keep conformance scope aligned with `docs/conformance/`.
- Tag slow, integration, and native-image tests explicitly.
- Do not add network access except in explicitly tagged integration tests.
- Do not vendor broad external W3C suites without the policy review described in
  `docs/conformance/w3c-test-suite-policy.md`.
