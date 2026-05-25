# generator-core

Schema compiler implementation module.

## Current status

Initial schema resource resolution, raw XSD syntax frontend behavior, component graph, normalized
IR, binding, generated model/writer/reader/validator emission, public generator-core adapter
behavior, generated-source verification harness behavior, and generated-code Native Image smoke
coverage are implemented for `XP-DATA-10`, `XP-DATA-10-CHOICE`, `XP-VALIDATION-10-BASIC`,
`XP-XSD10-COMPOSED`, `XP-XSD10-SEMANTIC`, `XP-XSD10-DOCUMENT`, and `XP-XSD10-FULL`.

The composed profile covers the accepted named model group and attribute-group flattening,
named list/union simple types, and initial derivation flattening slices. The semantic profile covers accepted
nillable/default/fixed behavior, direct substitution groups, and expanded generated validation for
those accepted semantic paths only. The document profile currently covers accepted direct
`xs:any` particles in sequences with retained `XmlFragment` wildcard fields, retained
`xs:anyAttribute` fields as immutable `List<XmlAttribute>` values, accepted mixed-content sequence
types, and verified stable project serialization policy. It does not claim W3C XML Canonicalization
or cryptographic canonical XML compatibility.

`TASK-0051` adds content-model coverage for required `xs:all`, all-optional `xs:all`, repeated
element-only choices, nested singleton sequences, and single-particle repeated/optional group refs.
`TASK-0065` enables `XP-XSD10-FULL` for accepted product-scope generated-binding shapes after the
implementation and initial W3C generated-binding mapping gates pass. `TASK-0066` adds the `1.0.0`
GitHub Release workflow and release evidence.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Use the generated-source verification harness for golden-output, deterministic emission, compile, JVM smoke, and generated-code native smoke coverage.
- Do not leak generator implementation dependencies into runtime modules or generated code.
