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
| Global/local `xs:element` | partially supported | `TASK-0049`, `TASK-0053`, `TASK-0061` | Abstract global substitution heads and nested/repeated substitution members are supported for accepted shapes; element `block` is enforced for accepted substitution and `xsi:type` dynamic-type paths. |
| Global/local `xs:attribute` | partially supported | `TASK-0052` | Local/global namespace qualification, refs, defaults/fixed, and prohibited use are supported for accepted shapes; derivation composition remains incomplete. |
| `xs:complexType` | partially supported | `TASK-0051`, `TASK-0053`, `TASK-0061` | Content-model support now includes legal `xs:all`, repeated choices, selected nested groups, simpleContent text-with-attributes binding, basic restriction member checks, abstract type metadata, final derivation checks, and known `xsi:type` dispatch. |
| `xs:simpleType` | partially supported | `TASK-0050`, `TASK-0053` | Needs full datatype, facet, list, union, and derivation semantics. |
| `xs:group` | partially supported | `TASK-0051`, `TASK-0059`, `TASK-0060` | Singleton refs and single-particle repeated/optional refs are flattened with composed cardinality; repeated/optional multi-particle groups bind as generated content-list shapes with shared reader/validator position metadata for nested choice positions. |
| `xs:attributeGroup` | partially supported | `TASK-0052` | Nested refs and anyAttribute composition are accepted for non-recursive groups; full restriction/derivation composition remains future. |
| `xs:notation` | tolerated metadata | `TASK-0049`, `TASK-0050`, `TASK-0063` | Parsed and indexed as a symbol-space component. NOTATION values use QName-compatible runtime lexical semantics for accepted scalar positions; declaration-table enforcement is not exposed as a generated public API. |
| Identity constraints | partially supported | `TASK-0049`, `TASK-0054` | `xs:unique`, `xs:key`, and `xs:keyref` are parsed, normalized, and enforced by generated document-scope validators for accepted generated model shapes. The accepted XPath subset covers namespace-aware QName steps, `*`, `.`, `.//`, `/`, union alternatives, and terminal attribute fields. Unsupported XPath axes, predicates, functions, parent traversal, variables, and arbitrary expressions remain deterministic diagnostics. |

## Particles and content models

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `xs:sequence` | supported for accepted shapes | `TASK-0051`, `TASK-0059`, `TASK-0060` | Direct sequences, nested singleton sequences, single-particle repeated/optional nested sequences, repeated/optional multi-particle groups, and nested choice positions inside grouped sequences are accepted for current generated shapes. |
| `xs:choice` | partially supported | `TASK-0051`, `TASK-0059` | Required, optional, and repeated element-only choices are generated; mixed choices and wildcard choice forms now bind as generated content lists where the surrounding profile accepts those branches. |
| `xs:all` | partially supported | `TASK-0049`, `TASK-0051`, `TASK-0059` | Required all-groups and optional all-groups whose members are all optional are generated with ordinary fields; optional all-groups with required children now bind as generated content-list models. |
| `xs:any` | partially supported | `TASK-0052`, `TASK-0059`, `TASK-0062` | Retained element fragments support accepted locations, namespace-token handling, process metadata, wildcard choice branch binding, and strict/lax schema-known validation for accepted retained declarations; remaining derivation interactions and W3C row mapping stay future. |
| `minOccurs`, `maxOccurs` | partially supported | `TASK-0051`, `TASK-0059`, `TASK-0060` | Cardinality composition is verified for single-particle nested sequences/group refs, repeated choices, grouped content-list shapes, and nested choice positions in grouped sequences. |
| UPA validation | partially supported | `TASK-0051`, `TASK-0060` | Element/wildcard and wildcard/wildcard overlap diagnostics are deterministic for accepted content-model positions; remaining full-suite UPA edge mapping continues under W3C generated-binding work. |
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
| `processContents=lax` and `strict` | partially supported | `TASK-0052`, `TASK-0062` | Metadata is parsed, bound, emitted, and tested for accepted retention paths; strict unknown matching names are rejected, lax unknown names are retained, and known retained element/attribute declarations are schema-validated through generated helpers. |

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
| `xs:list` | partially supported | `TASK-0050`, `TASK-0053`, `TASK-0063` | Named list item datatypes and anonymous simple restriction item members use the full datatype engine; optional/repeated list-valued fields and nested list composition stay outside accepted generated shapes. |
| `xs:union` | partially supported | `TASK-0050`, `TASK-0053`, `TASK-0063` | Named union members and anonymous simple restriction members use the full datatype engine; nested unions and richer generated value shapes stay outside accepted generated shapes. |

## Derivation and dynamic typing

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| `complexContent/extension` | partially supported | `TASK-0053` | Accepted named extensions flatten base-before-derived particles and attributes. |
| `complexContent/restriction` | partially supported | `TASK-0053`, `TASK-0062` | Basic restricted member checks and supported anyAttribute wildcard namespace narrowing diagnostics are implemented; complete particle, default/fixed, and broader attribute restriction algebra remains future. |
| `simpleContent/extension` | supported for accepted shapes | `TASK-0053` | Binds text value plus declared attributes for accepted simple bases. |
| `simpleContent/restriction` | partially supported | `TASK-0053` | Normalizes restrictions over accepted simple bases; full restriction against complex simple-content bases remains future. |
| Simple restriction derivation chains | partially supported | `TASK-0050`, `TASK-0053`, `TASK-0063` | Accepted scalar/list/union datatype chains and anonymous list/union restriction members use the datatype engine; deeper nested composition remains outside accepted generated shapes. |
| Abstract elements/types | partially supported | `TASK-0053`, `TASK-0061` | Abstract substitution heads and abstract complex types are represented in IR; generated binding omits abstract default dynamic branches and requires legal concrete substitution or `xsi:type` branches. |
| Substitution groups | partially supported | `TASK-0053`, `TASK-0061` | Direct, nested, repeated, and abstract-head substitution groups are supported for accepted branch shapes with deterministic cycle diagnostics; `block="substitution"` is enforced for accepted substitution heads. |
| `xsi:type` | partially supported | `TASK-0061` | Declared complex-base element fields bind to generated sealed branch interfaces when known concrete derived types exist; generated readers dispatch known `xsi:type`, writers emit `xsi:type` for derived branch values, validators recurse into concrete branch values, and unknown values produce deterministic diagnostics. Direct root-element `xsi:type` dispatch remains a later full-profile edge. |
| `xsi:nil` | partially supported | `TASK-0053`, `TASK-0063` | Required singleton nil model values are supported; nilled identity fields are treated as missing values for accepted identity constraints. Optional/repeated nil representation and broader derivation interactions remain outside accepted generated shapes. |

## Validation and conformance

| Construct | Current status | Target task | Notes |
|---|---|---|---|
| Structural generated validation | partially supported | `TASK-0051` | Reader, writer, and object validation now cover legal `xs:all`, repeated choices, and composed single-particle group cardinality for accepted shapes. |
| Datatype validation | supported for accepted schema shapes | `TASK-0050` | Full built-in lexical/value/facet coverage is verified for the currently executable schema shapes. |
| Identity constraints | partially supported | `TASK-0054`, `TASK-0063` | Generated validators build private document-scope identity tables for accepted model shapes, enforcing `unique`, `key`, and `keyref`; `TASK-0063` adds nilled-field handling. Broad W3C generated-binding mapping remains incomplete after `TASK-0064`. |
| W3C XML Schema 1.0 suite intake | supported as classification and mapped-row evidence | `TASK-0055`, `TASK-0064` | The pinned W3C 2007-06-20 suite archive is classified by the opt-in `w3cXsd10Conformance` lane. `TASK-0064` maps three `AttrDecl` rows to generated-binding support and executes one mapped generated-binding run; the remaining rows are still classification evidence until explicitly mapped or scoped out. |
| Full XSD 1.0 readiness claim | blocked by evidence | `TASK-0056`, `TASK-0058`, `TASK-0064` | `TASK-0056` reconciles the evidence and concludes that `XP-XSD10-FULL` must remain non-executable. `TASK-0058` defines the 1.0.0 blocker sequence. `TASK-0059` through `TASK-0064` have accepted grouped content-list, automata/UPA, dynamic typing, wildcard deep-validation, datatype/nil/identity-edge, and W3C generated-binding mapping gates; profile enablement (`TASK-0065`) and final release workflow/readiness (`TASK-0066`) remain before a full claim. |

## Explicit non-goals

XSD 1.1, XML 1.1, XML Canonicalization, XML Signature canonical forms, lexical prefix preservation,
DTD/entity identity preservation, and code-to-schema generation are not part of this program.
