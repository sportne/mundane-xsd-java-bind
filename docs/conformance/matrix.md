# Conformance matrix

| Feature | Standard area | Profile | Status | Tests | Notes |
|---|---|---|---|---|---|
| XML 1.0 well-formedness | XML 1.0 | `XP-DATA-10` | future | parser adapter tests | delegated to XML event source initially |
| Namespaces | Namespaces in XML | `XP-DATA-10` | designed | `T-NS-FRONTEND-*`, `T-NS-IR-*`, `T-NS-BIND-*` | raw syntax capture in `TASK-0007`; QName resolution in `TASK-0008`; package mapping in `TASK-0009` |
| `xs:element` simple | XSD 1.0 | `XP-DATA-10` | designed | `T-ELEMENT-FRONTEND-*`, `T-ELEMENT-IR-*`, `T-ELEMENT-BIND-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; root/field binding in `TASK-0009` |
| `xs:complexType` | XSD 1.0 | `XP-DATA-10` | designed | `T-COMPLEX-FRONTEND-*`, `T-COMPLEX-IR-*`, `T-COMPLEX-BIND-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; record-candidate binding in `TASK-0009` |
| `xs:sequence` | XSD 1.0 | `XP-DATA-10` | designed | `T-SEQUENCE-FRONTEND-*`, `T-SEQUENCE-IR-*`, `T-SEQUENCE-BIND-*` | raw syntax capture in `TASK-0007`; normalized IR in `TASK-0008`; sequence metadata in `TASK-0009` |
| `xs:choice` | XSD 1.0 | `XP-DATA-10-CHOICE` | future | `T-CHOICE-FRONTEND-*` | feasibility gate; `TASK-0007` emits unsupported-profile diagnostics |
| `xs:include`/`xs:import` | XSD 1.0 | `XP-DATA-10` | designed | `T-RES-*` | phase one resolver; first implementation card is `TASK-0006` |
| Schema resource policy | XML Base / XSD resource resolution | `XP-DATA-10` | designed | `T-RES-POLICY-*` | local roots and catalog mappings only unless explicitly configured |
| Network-denied resolver behavior | XML security | `XP-DATA-10` | designed | `T-SEC-RES-*` | default resolver must not open network resources |
| Include/import cycle diagnostics | XSD 1.0 | `XP-DATA-10` | designed | `T-RES-CYCLE-*` | deterministic diagnostic for recursive schema graphs |
| simple facets basic | XSD 1.0 Datatypes | `XP-VALIDATION-10-BASIC` | future | `T-FACET-*` | practical subset |
| substitution groups | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| identity constraints | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| XSD 1.1 assertions | XSD 1.1 | `XP-XSD11-ASSERT` | future | W3C/XPath subset | optional future |
