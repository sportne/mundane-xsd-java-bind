# Validation architecture

Validation is a first-class generated-code concern.

## Validation layers

| Layer | Phase-one behavior | Future behavior |
|---|---|---|
| Well-formedness | Delegated to XML event source. | Same unless custom parser is introduced. |
| Namespace correctness | Required for supported constructs. | Expanded edge cases. |
| Structural content model | Required for supported sequences/cardinality, legal `xs:all`, repeated choices, and grouped content-list models. | Derivation composition and W3C generated-binding row mapping. |
| Simple lexical conversion | Full XSD 1.0 built-ins for accepted schema shapes. | Broader schema shapes through later full-XSD gates. |
| Simple restrictions | XSD 1.0 facets for accepted schema shapes. | Integration with full derivation/content-model semantics. |
| Identity constraints | Generated document-scope validation for accepted model shapes. | Full-suite edge-case classification. |
| Defaults/fixed | Limited or validation-ready only. | Full schema semantics. |

## Diagnostics principles

- Failures must include a requirement/profile reason.
- Location should be included where available.
- Unsupported schema features must produce explicit profile diagnostics.
- Validation errors must be stable and testable.

## `TASK-0015` reader diagnostics baseline

Generated readers now report deterministic `XmlReadException` diagnostics for root/name mismatches, unexpected attributes/elements, missing required content, repeated singleton content, out-of-order sequence content, invalid scalar lexical values, and malformed event streams. Full generated validation behavior, including richer validation result APIs and facet semantics, remains staged for the basic validation task.

## `TASK-0016` generated validation baseline

Generated validators now return `ValidationResult` values for supported root models and XML input streams. Object validation covers required singleton values, repeated `minOccurs`, finite repeated `maxOccurs`, nested model aggregation, accepted simple restriction facets, and accepted `XP-XSD10-SEMANTIC` fixed-value checks with `XmlLocation.UNKNOWN`. XML validation delegates parsing and lexical checks to the generated reader, then applies object validation, preserving reader diagnostics when lexical conversion, nil-content, or fixed-value checks fail. `TASK-0054` adds generated document-scope identity validation for accepted `xs:unique`, `xs:key`, and `xs:keyref` shapes.

## `TASK-0024` facet validation scope

The `0.2.0` Practical Data Contracts slice accepts a narrow `XP-VALIDATION-10-BASIC` facet subset:
named simple-type restrictions over the existing supported scalar mappings, with
`xs:enumeration`, string length facets, numeric inclusive range facets, and `xs:pattern` for
`xs:string`.

Facet checks run in generated object validation and XML validation after reader lexical conversion
succeeds. Unsupported facets, list/union, derivation chains, anonymous simple types,
broader whitespace normalization, full date/time semantics, defaults/fixed, and
identity-constraint edge cases beyond accepted generated model shapes remain future
full-XSD-1.0 work with explicit diagnostics.

## `TASK-0027` through `TASK-0029` composed-schema validation

The `XP-XSD10-COMPOSED` validation behavior remains generated and explicit.

- `TASK-0027` flattened model groups and attribute groups reuse existing required, order,
  cardinality, and nested validation rules after normalization.
- `TASK-0028` list simple types validate each parsed item against the resolved item scalar or
  restricted alias and report deterministic generated validation errors for invalid items.
- `TASK-0028` union simple types validate that the lexical value matches at least one supported
  member parser and member facet rule.
- `TASK-0029` complex extension validates flattened base content before derived content in generated
  binding order; accepted simple restriction derivation chains validate merged facet metadata.
- Unsupported composition, list/union, and derivation cases remain schema diagnostics rather than
  partial generated validation behavior.

## `TASK-0050` datatype and facet engine

`TASK-0050` replaces the narrow scalar conversion baseline with a shared dependency-free XSD 1.0
datatype engine used by generated readers, writers, and validators for accepted schema shapes.
Runtime-core owns exact values for XML Schema datatypes that Java cannot represent directly,
including duration, date/time fragments, binary values, anyURI, and QName/NOTATION.

Generated validation enforces enumeration, pattern, length/minLength/maxLength, inclusive and
exclusive bounds, totalDigits, and fractionDigits through the shared datatype engine while keeping
stable generated diagnostic categories. QName parsing uses the active XML namespace context, and
generated writers emit QName lexical values through `XmlOutput` so adapters can declare deterministic
prefixes. `TASK-0051` adds content-model coverage for legal `xs:all`, repeated choices, nested
singleton sequences, and single-particle repeated/optional groups. `TASK-0052` adds accepted
attribute namespace qualification, prohibited-attribute rejection, retained `xs:anyAttribute`
validation, wildcard namespace-token matching, and `processContents` metadata. `TASK-0053` adds
accepted simpleContent text validation, basic complex restriction member checks before emission,
and generated validation recursion through repeated/nested/abstract substitution branch values.
Full restriction algebra, block/final, `xsi:type`, strict/lax schema-known wildcard validation,
identity-constraint edge cases beyond accepted generated model shapes, and `XP-XSD10-FULL`
execution remain future gates.

## `TASK-0032` semantic validation

The `XP-XSD10-SEMANTIC` validation behavior remains generated and explicit.

- `TASK-0032` validates nil content rules, rejects unsupported nil/default/fixed combinations, and
  checks fixed values for generated object and XML validation.
- `TASK-0032` readers produce effective scalar attribute default/fixed values when attributes are
  absent and preserve absent optional elements as absent rather than defaulted.
- `TASK-0033` validates substitution group dispatch using the resolved element name and generated
  sealed branch type.
- `TASK-0053` extends substitution validation to accepted abstract heads, nested substitution
  members, and repeated head references. Abstract head elements are not accepted as concrete
  branch values; generated validators recurse through each concrete branch value in list order.
- `TASK-0034` verifies deterministic semantic object diagnostics, location-aware XML diagnostics
  for nil-content and fixed-value reader failures, substitution branch value recursion, and
  explicit schema diagnostics for unsupported validation categories.
- Identity constraints are generated for accepted model shapes by `TASK-0054`; wildcard,
  mixed-content, and full XSD 1.0 validation edge cases remain future-profile work with explicit
  diagnostics.

## `TASK-0037` document wildcard validation

The `XP-XSD10-DOCUMENT` validation behavior remains generated and explicit.

- `TASK-0037` validates accepted wildcard cardinality, namespace constraints, and retained-fragment
  well-formed event structure after reader capture. `processContents="skip"` means generated
  validators do not schema-validate retained unknown fragments.
- `TASK-0038` validates mixed-content ordering against the generated content list and preserves
  deterministic diagnostics for missing, repeated, or out-of-order known elements.
- `TASK-0039` verifies stable serialization policy for accepted generated output and retained
  fragments without claiming formal XML Canonicalization.
- `TASK-0052` validates accepted `xs:anyAttribute` lists for null-free structure, namespace
  constraints, and prohibited/excluded names. Readers reject prohibited declared attributes before
  wildcard capture and continue to reject attributes that are neither declared nor accepted by the
  effective wildcard.
- `TASK-0059` validates generated grouped content-list fields for null-free structure, per-branch
  cardinality, retained wildcard namespace constraints, and deterministic list-order writer
  behavior. `TASK-0060` adds shared grouped-content position metadata so generated readers and
  validators agree for nested choice positions in grouped sequences, plus deterministic
  wildcard/wildcard UPA diagnostics.
- Full schema-known validation for `processContents="lax"` or `"strict"`, identity-constraint
  edge cases beyond accepted generated model shapes, complete derivation/restriction/block/final and
  `xsi:type` behavior, comments, processing instructions, entity-reference semantics, and full XSD
  1.0 validation remain future work.
