# Conformance matrix

| Feature | Standard area | Profile | Status | Tests | Notes |
|---|---|---|---|---|---|
| XML 1.0 well-formedness | XML 1.0 | `XP-DATA-10` | future | parser adapter tests | delegated to XML event source initially |
| Namespaces | Namespaces in XML | `XP-DATA-10` | designed | `T-NS-*` | required phase one |
| `xs:element` simple | XSD 1.0 | `XP-DATA-10` | designed | `T-ELEMENT-*` | phase one |
| `xs:complexType` | XSD 1.0 | `XP-DATA-10` | designed | `T-COMPLEX-*` | phase one |
| `xs:sequence` | XSD 1.0 | `XP-DATA-10` | designed | `T-SEQUENCE-*` | phase one |
| `xs:choice` | XSD 1.0 | `XP-DATA-10-CHOICE` | future | `T-CHOICE-*` | feasibility gate |
| `xs:include`/`xs:import` | XSD 1.0 | `XP-DATA-10` | designed | `T-RES-*` | phase one resolver |
| simple facets basic | XSD 1.0 Datatypes | `XP-VALIDATION-10-BASIC` | future | `T-FACET-*` | practical subset |
| substitution groups | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| identity constraints | XSD 1.0 | `XP-XSD10-FULL` | future | W3C subset | not phase one |
| XSD 1.1 assertions | XSD 1.1 | `XP-XSD11-ASSERT` | future | W3C/XPath subset | optional future |
