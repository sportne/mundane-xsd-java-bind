# Compatibility profiles

## XML/schema profiles

| Profile | Name | Meaning | Phase |
|---|---|---|---|
| `XP-DATA-10` | XSD 1.0 data-structure subset | Simple elements, complex types, attributes, nested elements, sequences, optional/repeated elements, namespaces, includes/imports, generated model/reader/writer/basic structural validation, and lexical conversion for the currently supported scalar types. | 1 |
| `XP-DATA-10-CHOICE` | Data subset with choices | Adds generated representation for feasible `xs:choice`. | 1/2 |
| `XP-VALIDATION-10-BASIC` | Basic generated validation | Expands validation beyond the first slice with practical simple-type facets such as enumeration, length/range, and pattern where accepted by planning. | 1/2 |
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
