# XSD 1.0 full feature matrix

This matrix defines the target surface for the `XP-XSD10-FULL` program. It is a planning and
traceability artifact, not a support claim. Status values:

- `supported`: implemented and covered by accepted tests.
- `tolerated/ignored`: parsed or carried without changing generated binding behavior.
- `diagnostic`: recognized and rejected deterministically.
- `not implemented yet`: known XSD 1.0 behavior that requires a future task.

## Schema and component model

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `xs:schema` | supported | done | XML 1.0 and Namespaces in XML 1.0 baseline only. |
| `targetNamespace`, `elementFormDefault`, `attributeFormDefault` | supported/tolerated | done/`TASK-0049` | Target namespaces are used by current binding; form defaults are parsed and preserved for later full binding semantics. |
| `blockDefault`, `finalDefault` | tolerated/ignored | `TASK-0049`, `TASK-0053` | Parsed and preserved; full derivation and substitution semantics remain future. |
| `xs:annotation`, `xs:documentation`, `xs:appinfo` | tolerated/ignored | `TASK-0049` | Parsed and preserved without changing binding behavior. |
| `xs:include`, `xs:import` | supported | done/`TASK-0049` | Direct and transitive chameleon includes now adopt the including namespace in the syntax/component model, with deterministic ambiguity/conflict diagnostics; imports remain resolver-backed. |
| `xs:redefine` | diagnostic | `TASK-0049`, `TASK-0053` | Recognized and rejected deterministically; component graph rewrite semantics remain future. |
| Global/local `xs:element` | partially supported | `TASK-0049`, `TASK-0053` | Abstract, block/final metadata is parsed; full substitution and dynamic type behavior remain incomplete. |
| Global/local `xs:attribute` | partially supported | `TASK-0052` | Prohibited use, full defaults/fixed, and wildcard composition remain incomplete. |
| `xs:complexType` | partially supported | `TASK-0051`, `TASK-0053` | Content-model support now includes legal `xs:all`, repeated choices, and selected nested groups; simpleContent, restriction, abstract types, and `xsi:type` remain future. |
| `xs:simpleType` | partially supported | `TASK-0050`, `TASK-0053` | Needs full datatype, facet, list, union, and derivation semantics. |
| `xs:group` | partially supported | `TASK-0051` | Singleton refs and single-particle repeated/optional refs are flattened with composed cardinality; repeated/optional multi-particle groups require a future grouped content-list shape. |
| `xs:attributeGroup` | partially supported | `TASK-0052` | Needs recursion checks, nested groups, and anyAttribute composition. |
| `xs:notation` | tolerated/ignored | `TASK-0049`, `TASK-0050` | Parsed and indexed as a symbol-space component; NOTATION datatype semantics remain future. |
| Identity constraints | diagnostic | `TASK-0049`, `TASK-0054` | `xs:unique`, `xs:key`, `xs:keyref`, selector, and field syntax are recognized and rejected before binding. |

## Particles and content models

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `xs:sequence` | supported for accepted shapes | `TASK-0051` | Direct sequences, nested singleton sequences, and single-particle repeated/optional nested sequences are accepted; repeated multi-particle groups remain diagnostic. |
| `xs:choice` | partially supported | `TASK-0051` | Required, optional, and repeated element-only choices are generated; mixed/wildcard choice forms remain future. |
| `xs:all` | partially supported | `TASK-0049`, `TASK-0051` | Required all-groups and optional all-groups whose members are all optional are generated with unordered reader acceptance and deterministic writer order; optional all-groups with required children remain diagnostic until grouped content state exists. |
| `xs:any` | partially supported | `TASK-0052` | Needs all locations, lax/strict processing, and derivation interactions. |
| `minOccurs`, `maxOccurs` | partially supported | `TASK-0051` | Cardinality composition is verified for single-particle nested sequences/group refs and repeated choices; grouped list semantics remain future. |
| UPA validation | partially supported | `TASK-0051` | Existing element/wildcard overlap diagnostics remain; complete automata-based UPA coverage remains future. |
| Mixed content | partially supported | `TASK-0051`, `TASK-0053` | Sequence mixed content remains supported; mixed choices and derivation interactions remain future. |

## Attributes and wildcards

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Attribute `use=optional|required` | supported for accepted shapes | `TASK-0052` | Complete with defaults/fixed and refs. |
| Attribute `use=prohibited` | not implemented yet | `TASK-0052` | Required for restriction and group composition. |
| Attribute defaults/fixed | partially supported | `TASK-0052` | Needs full datatype and derivation interactions. |
| `xs:anyAttribute` | diagnostic | `TASK-0049`, `TASK-0052` | Syntax is recognized and rejected before binding; retained unknown attributes remain future. |
| Wildcard namespace constraints | partially supported | `TASK-0052` | Complete union/intersection/subset semantics. |
| `processContents=skip` | partially supported | `TASK-0052` | Current support is direct element wildcard only. |
| `processContents=lax` and `strict` | diagnostic | `TASK-0052` | Requires schema-known dispatch/validation policy. |

## Simple types and facets

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Primitive string/boolean/decimal/integer family | supported for accepted schema shapes | `TASK-0050` | Runtime lexical conversion, generated mapping, writer output, and validator facets are verified. |
| Primitive float/double | supported for accepted schema shapes | `TASK-0050` | Includes `NaN`, `INF`, and `-INF` lexical handling. |
| Primitive duration/dateTime/time/date/g* types | supported for accepted schema shapes | `TASK-0050` | Project-owned immutable runtime values retain lexical timezone-presence semantics. |
| Primitive hexBinary/base64Binary | supported for accepted schema shapes | `TASK-0050` | `XmlBinary` retains immutable bytes and emits stable hex/base64 lexical forms. |
| Primitive anyURI | supported for accepted schema shapes | `TASK-0050` | `XmlAnyUri` preserves the accepted lexical URI value. |
| Primitive QName/NOTATION | supported for accepted schema shapes | `TASK-0050` | QName values use expanded-name equality; generated element parsing preserves namespace context before advancing. |
| Derived string types | supported for accepted schema shapes | `TASK-0050` | normalizedString, token, language, Name, NCName, ID, IDREF, ENTITY, NMTOKEN families plus list-valued built-ins. |
| Derived numeric types | supported for accepted schema shapes | `TASK-0050` | long/int/short/byte, unsigned types, positive/negative variants map to exact Java numeric types where safe. |
| Restriction facets | supported for accepted schema shapes | `TASK-0050` | length, min/max length, pattern, enumeration, whiteSpace metadata, inclusive/exclusive bounds, totalDigits, and fractionDigits. |
| `xs:list` | partially supported | `TASK-0050`, `TASK-0053` | Named list item datatypes use the full datatype engine; optional/repeated fields, anonymous item types, and nested composition rules remain future. |
| `xs:union` | partially supported | `TASK-0050`, `TASK-0053` | Named union members use the full datatype engine; anonymous members, nested unions, and richer generated value shapes remain future. |

## Derivation and dynamic typing

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `complexContent/extension` | partially supported | `TASK-0053` | Current implementation flattens accepted named extensions only. |
| `complexContent/restriction` | diagnostic/not implemented yet | `TASK-0053` | Requires particle and attribute restriction checks. |
| `simpleContent/extension` | diagnostic/not implemented yet | `TASK-0053` | Required for text-with-attributes binding. |
| `simpleContent/restriction` | diagnostic/not implemented yet | `TASK-0053` | Requires base simple content validation. |
| Simple restriction derivation chains | partially supported | `TASK-0050`, `TASK-0053` | Needs all facets and built-ins. |
| Abstract elements/types | diagnostic/not implemented yet | `TASK-0053` | Requires generated polymorphic model shapes. |
| Substitution groups | partially supported | `TASK-0053` | Needs abstract heads, nested groups, cycles, repeated heads, block/final. |
| `xsi:type` | not implemented yet | `TASK-0053` | Requires known derived-type dispatch and validation. |
| `xsi:nil` | partially supported | `TASK-0053` | Needs optional/repeated and derivation interactions. |

## Validation and conformance

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Structural generated validation | partially supported | `TASK-0051` | Reader, writer, and object validation now cover legal `xs:all`, repeated choices, and composed single-particle group cardinality for accepted shapes. |
| Datatype validation | supported for accepted schema shapes | `TASK-0050` | Full built-in lexical/value/facet coverage is verified for the currently executable schema shapes. |
| Identity constraints | diagnostic | `TASK-0054` | Requires document-scope validation context. |
| W3C XML Schema 1.0 suite intake | blocked/planned | `TASK-0055` | Must be pinned, classified, and locally repeatable. |
| Full XSD 1.0 readiness claim | not implemented yet | `TASK-0056` | Only after matrix evidence passes. |

## Explicit non-goals

XSD 1.1, XML 1.1, XML Canonicalization, XML Signature canonical forms, lexical prefix preservation,
DTD/entity identity preservation, and code-to-schema generation are not part of this program.
