# Validation architecture

Validation is a first-class generated-code concern.

## Validation layers

| Layer | Phase-one behavior | Future behavior |
|---|---|---|
| Well-formedness | Delegated to XML event source. | Same unless custom parser is introduced. |
| Namespace correctness | Required for supported constructs. | Expanded edge cases. |
| Structural content model | Required for supported sequences/cardinality. | Full model groups and derivation. |
| Simple lexical conversion | Common built-ins. | Full XSD datatype system. |
| Simple restrictions | Practical facets: enum, length, range, pattern where feasible. | Complete facets, list/union. |
| Identity constraints | Validation-ready only. | `xs:key`, `xs:keyref`, `xs:unique`. |
| Defaults/fixed | Limited or validation-ready only. | Full schema semantics. |
| XSD 1.1 assertions | Out of scope initially. | Optional XPath-backed profile. |

## Diagnostics principles

- Failures must include a requirement/profile reason.
- Location should be included where available.
- Unsupported schema features must produce explicit profile diagnostics.
- Validation errors must be stable and testable.
