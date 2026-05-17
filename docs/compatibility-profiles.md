# Compatibility profiles

## XML/schema profiles

| Profile | Name | Meaning | Phase |
|---|---|---|---|
| `XP-DATA-10` | XSD 1.0 data-structure subset | Simple elements, complex types, attributes, nested elements, sequences, optional/repeated elements, namespaces, includes/imports, generated model/reader/writer/basic structural validation, and lexical conversion for the currently supported scalar types. | 1 |
| `XP-DATA-10-CHOICE` | Data subset with choices | Opt-in `0.2.0` extension for local singleton `xs:choice` particles with local or referenced supported element branches, generated sealed choice model types, reader/writer support, and explicit diagnostics for out-of-scope model-group shapes. | 2 |
| `XP-VALIDATION-10-BASIC` | Basic generated validation | Planned `0.2.0` extension for named simple-type restrictions using accepted enumeration, string length, numeric inclusive range, and string pattern facets over already supported scalar bases. | 2 |
| `XP-XSD10-FULL` | Full XSD 1.0 | Substitution groups, derivation, wildcards, identity constraints, nillable, default/fixed, mixed content. | Future |
| `XP-XSD11-ASSERT` | XSD 1.1 assertions | XSD 1.1 features including assertions and conditional alternatives. | Future |
| `XP-XML11` | XML 1.1 | XML 1.1 parsing/serialization compatibility. | Future |
| `XP-INTEROP` | Interoperability | Round-trip and validation comparison with existing XML tooling. | Ongoing |

## Java/runtime profiles

| Profile | Meaning |
|---|---|
| `JP-21-MAIN` | Java 21 source/release baseline. |
| `JP-25-COMPAT` | Compatibility lane using Java 25. |
| `RT-CORE-ZERO-THIRD-PARTY` | Generated code plus runtime core have no third-party dependencies. |
| `RT-JDKXML-ADAPTER` | Optional adapter using JDK XML APIs behind project-owned interfaces. |
| `NI-SMOKE` | Native Image smoke tests for generated bindings. |
| `NI-CONFORMANCE` | Native Image execution of selected round-trip/conformance tests. |

## Build profiles

| Profile | Meaning |
|---|---|
| `BUILD-ONLINE` | Normal developer build with remote repositories. |
| `BUILD-OFFLINE-HYDRATED` | Offline build using hydrated local Maven repo and provisioned Gradle distribution. |
| `BUILD-CI-MATRIX` | CI on Java 21 and Java 25. |
| `BUILD-STRICT` | Full quality, architecture, coverage, docs, generated-code, and native-image gates. |

## `0.2.0` Planning Baseline

`TASK-0022` accepts the Practical Data Contracts plan but does not create a release tag or
publication claim. `TASK-0023` accepts `XP-DATA-10-CHOICE` as an opt-in profile; default
`XP-DATA-10` still rejects `xs:choice`. Until `TASK-0024` is accepted,
`XP-VALIDATION-10-BASIC` remains a designed profile with expected diagnostics for unsupported input.

The `XP-DATA-10-CHOICE` implementation scope is limited to local `xs:choice` particles in a complex
type, either as the only content particle or inside an existing supported sequence. Accepted choices
use `minOccurs` of `0` or `1`, `maxOccurs` of `1`, and singleton local or referenced element
branches whose resolved types are already supported by `XP-DATA-10`.

The `XP-VALIDATION-10-BASIC` implementation scope is limited to named `xs:simpleType` restrictions
over `xs:string`, `xs:boolean`, `xs:int`, `xs:integer`, `xs:long`, and `xs:decimal`, with accepted
enumeration, string length, numeric inclusive range, and string pattern facets. Full model groups,
list/union simple types, derivation chains, full datatype semantics, and `XP-XSD10-FULL` remain
future work.
