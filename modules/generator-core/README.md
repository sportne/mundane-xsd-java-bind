# generator-core

Schema compiler implementation module.

## Current status

Initial schema resource resolution, raw XSD syntax frontend behavior, component graph, normalized
IR, binding, generated model/writer/reader/validator emission, public generator-core adapter
behavior, generated-source verification harness behavior, and generated-code Native Image smoke
coverage are implemented for the accepted `XP-DATA-10` slice and the accepted opt-in
`XP-DATA-10-CHOICE`, `XP-VALIDATION-10-BASIC`, `XP-XSD10-COMPOSED`, and
`XP-XSD10-SEMANTIC` subsets, plus accepted direct wildcard/open-content support for
`XP-XSD10-DOCUMENT`.

The composed profile covers the accepted named model group and attribute-group flattening,
named list/union simple types, and initial derivation flattening slices only. Full XSD 1.0
composition remains future profile work. The semantic profile covers accepted
nillable/default/fixed behavior, direct substitution groups, and expanded generated validation for
those accepted semantic paths only. The document profile currently covers accepted direct
`xs:any` particles in sequences with retained `XmlFragment` wildcard fields, accepted mixed-content
sequence types, and verified stable project serialization policy. It does not claim W3C XML
Canonicalization or cryptographic canonical XML compatibility.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Use the generated-source verification harness for golden-output, deterministic emission, compile, JVM smoke, and generated-code native smoke coverage.
- Do not leak generator implementation dependencies into runtime modules or generated code.
