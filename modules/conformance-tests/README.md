# conformance-tests

Internal conformance test harness; not published.

## Current status

Selected local `XP-DATA-10` fixtures exercise the first public vertical slice without claiming full
W3C XML Schema conformance. The tests reuse the checked-in generated fixture sources from the
purchase-order and multi-namespace examples.

Covered behavior:

- XML to object to XML round trips for representative generated bindings.
- Namespace-aware element matching across imported-schema fixture boundaries.
- Required-content, out-of-order sequence, namespace mismatch, and scalar lexical diagnostics.
- Opt-in `xs:choice`, `XP-VALIDATION-10-BASIC` facet, `XP-XSD10-COMPOSED` composed-schema,
  `XP-XSD10-SEMANTIC` semantic, and accepted `XP-XSD10-DOCUMENT` wildcard, mixed-content, and
  serialization-policy fixtures compared with JDK XML Schema validation where practical.

Unsupported or future-profile constructs such as full simple-type facets beyond
`XP-VALIDATION-10-BASIC`, full derivation semantics, wildcard behavior beyond accepted direct
`xs:any`, mixed-content shapes beyond accepted direct sequences, identity constraints, and XSD 1.1
are outside this harness until their task cards are accepted.

## Contributor notes

- Keep conformance scope aligned with `docs/conformance/`.
- Tag slow, integration, and native-image tests explicitly.
- Do not add network access except in explicitly tagged integration tests.
- Do not vendor broad external W3C suites without the policy review described in
  `docs/conformance/w3c-test-suite-policy.md`.
