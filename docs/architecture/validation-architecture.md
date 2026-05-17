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

## `TASK-0015` reader diagnostics baseline

Generated readers now report deterministic `XmlReadException` diagnostics for root/name mismatches, unexpected attributes/elements, missing required content, repeated singleton content, out-of-order sequence content, invalid scalar lexical values, and malformed event streams. Full generated validation behavior, including richer validation result APIs and facet semantics, remains staged for the basic validation task.

## `TASK-0016` generated validation baseline

Generated validators now return `ValidationResult` values for supported root models and XML input streams. Object validation covers required singleton values, repeated `minOccurs`, finite repeated `maxOccurs`, nested model aggregation, and accepted `XP-VALIDATION-10-BASIC` simple restriction facets with `XmlLocation.UNKNOWN`. XML validation delegates parsing and lexical checks to the generated reader, then applies object validation, preserving reader diagnostics when lexical conversion fails. Defaults/fixed semantics, identity constraints, and expanded datatype validation remain future validation phases.

## `TASK-0024` facet validation scope

The `0.2.0` Practical Data Contracts slice accepts a narrow `XP-VALIDATION-10-BASIC` facet subset:
named simple-type restrictions over the existing supported scalar mappings, with
`xs:enumeration`, string length facets, numeric inclusive range facets, and `xs:pattern` for
`xs:string`.

Facet checks run in generated object validation and XML validation after reader lexical conversion
succeeds. Unsupported facets, list/union, derivation chains, anonymous simple types,
broader whitespace normalization, full date/time semantics, identity constraints, defaults/fixed,
and XSD 1.1 assertions remain future-profile work with explicit diagnostics.
