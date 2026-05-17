# Conformance matrix

| Feature | Standard area | Profile | Status | Tests | Notes |
|---|---|---|---|---|---|
| XML 1.0 well-formedness | XML 1.0 | `XP-DATA-10` | designed | parser adapter tests, `T-READER-*` | delegated to XML event source; generated readers consume well-formed event streams |
| Namespaces | Namespaces in XML | `XP-DATA-10` | designed | `T-NS-FRONTEND-*`, `T-NS-IR-*`, `T-NS-BIND-*`, `T-NS-WRITER-*`, `T-NS-READER-*` | raw syntax capture in `TASK-0007`; QName resolution in `TASK-0008`; package mapping in `TASK-0009`; writer `XmlName` constants in `TASK-0012`; reader namespace matching in `TASK-0015` |
| `xs:element` simple | XSD 1.0 | `XP-DATA-10` | designed | `T-ELEMENT-FRONTEND-*`, `T-ELEMENT-IR-*`, `T-ELEMENT-BIND-*`, `T-ELEMENT-WRITER-*`, `T-ELEMENT-READER-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; root/field binding in `TASK-0009`; generated writer output in `TASK-0012`; generated reader input in `TASK-0015` |
| `xs:complexType` | XSD 1.0 | `XP-DATA-10` | designed | `T-COMPLEX-FRONTEND-*`, `T-COMPLEX-IR-*`, `T-COMPLEX-BIND-*`, `T-COMPLEX-WRITER-*`, `T-COMPLEX-READER-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; record-candidate binding in `TASK-0009`; nested writer helpers in `TASK-0012`; nested reader helpers in `TASK-0015` |
| `xs:sequence` | XSD 1.0 | `XP-DATA-10` | designed | `T-SEQUENCE-FRONTEND-*`, `T-SEQUENCE-IR-*`, `T-SEQUENCE-BIND-*`, `T-SEQUENCE-WRITER-*`, `T-SEQUENCE-READER-*`, `T-VALIDATOR-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; sequence metadata in `TASK-0009`; writer element order in `TASK-0012`; reader order/cardinality diagnostics in `TASK-0015`; generated validator object/XML result diagnostics in `TASK-0016` |
| `xs:choice` | XSD 1.0 | `XP-DATA-10-CHOICE` | future | `T-CHOICE-FRONTEND-*` | feasibility gate; `TASK-0007` emits unsupported-profile diagnostics |
| `xs:include`/`xs:import` | XSD 1.0 | `XP-DATA-10` | designed | `T-RES-*` | phase one resolver; first implementation card is `TASK-0006` |
| Schema resource policy | XML Base / XSD resource resolution | `XP-DATA-10` | designed | `T-RES-POLICY-*` | local roots and catalog mappings only unless explicitly configured |
| Network-denied resolver behavior | XML security | `XP-DATA-10` | designed | `T-SEC-RES-*` | default resolver must not open network resources |
| Include/import cycle diagnostics | XSD 1.0 | `XP-DATA-10` | designed | `T-RES-CYCLE-*` | deterministic diagnostic for recursive schema graphs |
| basic validation results | XSD 1.0 validation | `XP-DATA-10` | designed | `T-VALIDATOR-*` | generated `ValidationResult` support for required values, repeated cardinality, nested aggregation, and preserved reader diagnostics in `TASK-0016` |
| simple facets basic | XSD 1.0 Datatypes | `XP-VALIDATION-10-BASIC` | future | `T-FACET-*` | practical subset beyond TASK-0016 |
| substitution groups | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| identity constraints | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| XSD 1.1 assertions | XSD 1.1 | `XP-XSD11-ASSERT` | future | W3C/XPath subset | optional future |
