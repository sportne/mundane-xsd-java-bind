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
| `targetNamespace`, `elementFormDefault`, `attributeFormDefault` | supported | done/`TASK-0049`/`TASK-0052` | Target namespaces and form defaults are used by accepted element and attribute binding paths. |
| `blockDefault`, `finalDefault` | tolerated/ignored | `TASK-0049`, `TASK-0053` | Parsed and preserved; full derivation and substitution semantics remain future. |
| `xs:annotation`, `xs:documentation`, `xs:appinfo` | tolerated/ignored | `TASK-0049` | Parsed and preserved without changing binding behavior. |
| `xs:include`, `xs:import` | supported | done/`TASK-0049` | Direct and transitive chameleon includes now adopt the including namespace in the syntax/component model, with deterministic ambiguity/conflict diagnostics; imports remain resolver-backed. |
| `xs:redefine` | diagnostic | `TASK-0049`, `TASK-0053` | Recognized and rejected deterministically; component graph rewrite semantics remain future. |
| Global/local `xs:element` | partially supported | `TASK-0049`, `TASK-0053` | Abstract global substitution heads and nested/repeated substitution members are supported for accepted shapes; block/final and `xsi:type` dynamic type behavior remain incomplete. |
| Global/local `xs:attribute` | partially supported | `TASK-0052` | Local/global namespace qualification, refs, defaults/fixed, and prohibited use are supported for accepted shapes; derivation composition remains incomplete. |
| `xs:complexType` | partially supported | `TASK-0051`, `TASK-0053` | Content-model support now includes legal `xs:all`, repeated choices, selected nested groups, simpleContent text-with-attributes binding, and basic restriction member checks; full restriction algebra, abstract type instantiation rules, and `xsi:type` remain future. |
| `xs:simpleType` | partially supported | `TASK-0050`, `TASK-0053` | Needs full datatype, facet, list, union, and derivation semantics. |
| `xs:group` | partially supported | `TASK-0051`, `TASK-0059` | Singleton refs and single-particle repeated/optional refs are flattened with composed cardinality; repeated/optional multi-particle groups whose members are singleton particles now bind as generated content-list shapes, with nested/non-singleton child-particle automata still deferred to `TASK-0060`. |
| `xs:attributeGroup` | partially supported | `TASK-0052` | Nested refs and anyAttribute composition are accepted for non-recursive groups; full restriction/derivation composition remains future. |
| `xs:notation` | tolerated/ignored | `TASK-0049`, `TASK-0050` | Parsed and indexed as a symbol-space component; NOTATION datatype semantics remain future. |
| Identity constraints | partially supported | `TASK-0049`, `TASK-0054` | `xs:unique`, `xs:key`, and `xs:keyref` are parsed, normalized, and enforced by generated document-scope validators for accepted generated model shapes. The accepted XPath subset covers namespace-aware QName steps, `*`, `.`, `.//`, `/`, union alternatives, and terminal attribute fields. Unsupported XPath axes, predicates, functions, parent traversal, variables, and arbitrary expressions remain deterministic diagnostics. |

## Particles and content models

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `xs:sequence` | supported for accepted shapes | `TASK-0051`, `TASK-0059` | Direct sequences, nested singleton sequences, single-particle repeated/optional nested sequences, and repeated/optional multi-particle groups whose children are singleton particles are accepted; grouped multi-particle cases use generated content-list models and deeper nested automata remain `TASK-0060`. |
| `xs:choice` | partially supported | `TASK-0051`, `TASK-0059` | Required, optional, and repeated element-only choices are generated; mixed choices and wildcard choice forms now bind as generated content lists where the surrounding profile accepts those branches. |
| `xs:all` | partially supported | `TASK-0049`, `TASK-0051`, `TASK-0059` | Required all-groups and optional all-groups whose members are all optional are generated with ordinary fields; optional all-groups with required children now bind as generated content-list models. |
| `xs:any` | partially supported | `TASK-0052`, `TASK-0059` | Retained element fragments support accepted locations, namespace-token handling, process metadata, and wildcard choice branch binding; strict/lax schema-known deep validation and derivation interactions remain future. |
| `minOccurs`, `maxOccurs` | partially supported | `TASK-0051`, `TASK-0059` | Cardinality composition is verified for single-particle nested sequences/group refs, repeated choices, and grouped content-list shapes with singleton child particles; complete automata execution remains future. |
| UPA validation | partially supported | `TASK-0051` | Existing element/wildcard overlap diagnostics remain; complete automata-based UPA coverage remains future. |
| Mixed content | partially supported | `TASK-0051`, `TASK-0053`, `TASK-0059` | Sequence mixed content and mixed choices now use generated content-list models; mixed derivation interactions remain future. |

## Attributes and wildcards

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Attribute `use=optional|required` | supported for accepted shapes | `TASK-0052` | Includes refs, local/global namespace qualification, defaults, and fixed values for accepted scalar shapes. |
| Attribute `use=prohibited` | supported for accepted shapes | `TASK-0052` | Prohibited attributes are not public model fields and are rejected before anyAttribute capture. |
| Attribute defaults/fixed | partially supported | `TASK-0052` | Accepted scalar defaults/fixed are supported; full derivation interactions remain future. |
| `xs:anyAttribute` | partially supported | `TASK-0049`, `TASK-0052` | Retained unknown attributes bind as immutable `List<XmlAttribute>` values for accepted shapes. |
| Wildcard namespace constraints | partially supported | `TASK-0052` | `##any`, `##other`, `##local`, `##targetNamespace`, and explicit URI-list matching are supported; full derivation algebra remains future. |
| `processContents=skip` | partially supported | `TASK-0052` | Supported for retained element and attribute wildcards in accepted shapes. |
| `processContents=lax` and `strict` | partially supported | `TASK-0052` | Metadata is parsed, bound, emitted, and tested for accepted retention paths; full schema-known deep validation remains future. |

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
| `complexContent/extension` | partially supported | `TASK-0053` | Accepted named extensions flatten base-before-derived particles and attributes. |
| `complexContent/restriction` | partially supported | `TASK-0053` | Basic restricted member checks are implemented; complete particle, wildcard, default/fixed, and attribute restriction algebra remains future. |
| `simpleContent/extension` | supported for accepted shapes | `TASK-0053` | Binds text value plus declared attributes for accepted simple bases. |
| `simpleContent/restriction` | partially supported | `TASK-0053` | Normalizes restrictions over accepted simple bases; full restriction against complex simple-content bases remains future. |
| Simple restriction derivation chains | partially supported | `TASK-0050`, `TASK-0053` | Accepted scalar/list/union datatype chains use the datatype engine; anonymous and deeper composition rules remain future. |
| Abstract elements/types | partially supported | `TASK-0053` | Abstract substitution heads are omitted from concrete branch models and rejected when no concrete members exist; full abstract complex type and `xsi:type` behavior remains future. |
| Substitution groups | partially supported | `TASK-0053` | Direct, nested, repeated, and abstract-head substitution groups are supported for accepted branch shapes with deterministic cycle diagnostics; block/final remains future. |
| `xsi:type` | not implemented yet | `TASK-0053` | Requires known derived-type dispatch and validation in a later full-XSD gate. |
| `xsi:nil` | partially supported | `TASK-0053` | Needs optional/repeated and derivation interactions. |

## Validation and conformance

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Structural generated validation | partially supported | `TASK-0051` | Reader, writer, and object validation now cover legal `xs:all`, repeated choices, and composed single-particle group cardinality for accepted shapes. |
| Datatype validation | supported for accepted schema shapes | `TASK-0050` | Full built-in lexical/value/facet coverage is verified for the currently executable schema shapes. |
| Identity constraints | partially supported | `TASK-0054` | Generated validators build private document-scope identity tables for accepted model shapes, enforcing `unique`, `key`, and `keyref`; full-suite coverage and edge-case conformance classification remain `TASK-0055`. |
| W3C XML Schema 1.0 suite intake | supported as classification evidence | `TASK-0055` | The pinned W3C 2007-06-20 suite archive is classified by the opt-in `w3cXsd10Conformance` lane. Current evidence covers 24,796 schema/instance documents with zero generated-binding support claims until rows are explicitly mapped. |
| Full XSD 1.0 readiness claim | blocked by evidence | `TASK-0056`, `TASK-0058` | `TASK-0056` reconciles the evidence and concludes that `XP-XSD10-FULL` must remain non-executable. `TASK-0058` defines the 1.0.0 blocker sequence: grouped content-list models (`TASK-0059`), complete automata/UPA (`TASK-0060`), derivation/dynamic typing (`TASK-0061`), wildcard deep validation (`TASK-0062`), datatype/nil/identity edges (`TASK-0063`), W3C generated-binding mapping (`TASK-0064`), profile enablement (`TASK-0065`), and final release workflow/readiness (`TASK-0066`). |

## Explicit non-goals

XSD 1.1, XML 1.1, XML Canonicalization, XML Signature canonical forms, lexical prefix preservation,
DTD/entity identity preservation, and code-to-schema generation are not part of this program.
