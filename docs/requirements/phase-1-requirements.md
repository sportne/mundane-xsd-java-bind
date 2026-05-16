# Phase-one requirements

## Scope and runtime

| ID | Requirement | Verification |
|---|---|---|
| `REQ-SCOPE-001` | The project shall generate Java code from XML Schema documents only. | CLI/Gradle API tests; no code-to-schema tasks. |
| `REQ-SCOPE-002` | The project shall not generate XML Schema documents from Java code. | Build and API surface review. |
| `REQ-RT-001` | Runtime core and generated code shall have no third-party dependencies. | ArchUnit and dependency reports. |
| `REQ-RT-002` | Generated readers/writers shall call explicit generated code, not reflection-based mappers. | ArchUnit, source inspection, Native Image tests. |

## Schema subset

| ID | Requirement | Verification |
|---|---|---|
| `REQ-SCHEMA-001` | Support global and local simple elements in profile `XP-DATA-10`. | Golden IR and round-trip tests. |
| `REQ-SCHEMA-002` | Support complex types with attributes and nested elements. | Golden IR and generated compile tests. |
| `REQ-SCHEMA-003` | Support `xs:sequence`. | Valid/invalid sequence tests. |
| `REQ-SCHEMA-004` | Support optional/repeated elements via `minOccurs` and `maxOccurs`. | Cardinality tests. |
| `REQ-SCHEMA-005` | Support `xs:include` and `xs:import` through explicit resolver policy. | Resolver tests with offline fixtures. |
| `REQ-SCHEMA-006` | Support namespaces and QNames for elements/types/attributes. | Multi-namespace examples. |
| `REQ-SCHEMA-007` | Support feasible `xs:choice` representation if approved for phase one. | Choice binding tests or profile-gated unsupported diagnostics. |

## Binding and generated code

| ID | Requirement | Verification |
|---|---|---|
| `REQ-NS-001` | Namespace-to-package mapping shall be deterministic and externally configurable. | Binding config tests. |
| `REQ-GEN-001` | Code generation shall be deterministic byte-for-byte for equivalent inputs and config. | Golden source tests. |
| `REQ-GEN-002` | Generated source shall pass formatting and static-analysis gates. | Compile/static-analysis tests. |
| `REQ-MODEL-001` | Generated models shall be immutable by default. | Source and behavior tests. |
| `REQ-XML-W-001` | Generated writers shall produce namespace-correct XML for supported constructs. | XML output tests. |
| `REQ-XML-R-001` | Generated readers shall produce generated model instances for supported XML. | XML input tests. |

## Validation and diagnostics

| ID | Requirement | Verification |
|---|---|---|
| `REQ-VAL-001` | Readers shall report supported schema-profile violations with location where available. | Negative XML tests. |
| `REQ-VAL-002` | Basic structural validation shall cover required fields, order, and cardinality for supported sequences. | Negative structural tests. |
| `REQ-VAL-003` | Basic lexical validation shall cover common primitive simple types. | Simple type tests. |

## Build and governance

| ID | Requirement | Verification |
|---|---|---|
| `REQ-BUILD-001` | The build shall use Gradle Groovy DSL. | Build review. |
| `REQ-BUILD-002` | The Java baseline shall be Java 21. | Toolchain config and CI. |
| `REQ-BUILD-003` | Java 25 compatibility shall be tested in CI. | CI matrix. |
| `REQ-QA-001` | The build shall include Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, and JaCoCo. | Build tasks and CI. |
| `REQ-NI-001` | Generated sample bindings shall compile and execute under Native Image smoke tests. | Native workflow. |
| `REQ-AGENT-001` | Agents shall operate only through approved task cards and file scopes. | PR review. |
