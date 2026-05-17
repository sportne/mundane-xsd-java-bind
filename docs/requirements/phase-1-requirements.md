# Phase-one requirements

Phase-one readiness status: verified for the first public `XP-DATA-10` vertical slice.
`REQ-SCHEMA-007` has opt-in `XP-DATA-10-CHOICE` acceptance evidence from `TASK-0023`; default
`XP-DATA-10` still produces explicit unsupported-profile diagnostics for `xs:choice`.

## Scope and runtime

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-SCOPE-001` | The project shall generate Java code from XML Schema documents only. | CLI/Gradle API tests; no code-to-schema tasks. | verified |
| `REQ-SCOPE-002` | The project shall not generate XML Schema documents from Java code. | Build and API surface review. | verified |
| `REQ-RT-001` | Runtime core and generated code shall have no third-party dependencies. | ArchUnit and dependency reports. | verified |
| `REQ-RT-002` | Generated readers/writers shall call explicit generated code, not reflection-based mappers. | ArchUnit, source inspection, Native Image tests. | verified |

## Schema subset

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-SCHEMA-001` | Support global and local simple elements in profile `XP-DATA-10`. | Golden IR and round-trip tests. | verified |
| `REQ-SCHEMA-002` | Support complex types with attributes and nested elements. | Golden IR and generated compile tests. | verified |
| `REQ-SCHEMA-003` | Support `xs:sequence`. | Valid/invalid sequence tests. | verified |
| `REQ-SCHEMA-004` | Support optional/repeated elements via `minOccurs` and `maxOccurs`. | Cardinality tests. | verified |
| `REQ-SCHEMA-005` | Support `xs:include` and `xs:import` through explicit resolver policy. | Resolver tests with offline fixtures. | verified |
| `REQ-SCHEMA-006` | Support namespaces and QNames for elements/types/attributes. | Multi-namespace examples. | verified |
| `REQ-SCHEMA-007` | Keep `xs:choice` behind profile `XP-DATA-10-CHOICE`; support local singleton choice particles with supported element branches for `0.2.0`. | `TASK-0023` frontend, IR, binding, generated-code, CoreGenerator, CLI, and Gradle plugin tests; default profile unsupported diagnostics; conformance/interop fixtures compared with JDK XML Schema validation; representative choice path in generated-code smoke fixtures. | verified |

## Resource resolution and security

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-RES-001` | Schema include/import resolution shall use explicit resolver policy with local file/resource roots and catalog mappings; implicit network resolution is forbidden. | Resolver tests with offline fixtures and catalog mappings. | verified |
| `REQ-SEC-001` | Schema resource resolution shall deny network access by default and report include/import cycles with deterministic diagnostics. | Negative resolver tests for denied network URI and cycle fixtures. | verified |

## Binding and generated code

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-NS-001` | Namespace-to-package mapping shall be deterministic and externally configurable. | Binding config tests. | verified |
| `REQ-GEN-001` | Code generation shall be deterministic byte-for-byte for equivalent inputs and config. | Golden source tests. | verified |
| `REQ-GEN-002` | Generated source shall pass formatting and static-analysis gates. | Compile/static-analysis tests. | verified |
| `REQ-MODEL-001` | Generated models shall be immutable by default. | Source and behavior tests. | verified |
| `REQ-XML-W-001` | Generated writers shall produce namespace-correct XML for supported constructs. | XML output tests. | verified |
| `REQ-XML-R-001` | Generated readers shall produce generated model instances for supported XML. | XML input tests. | verified |

## Validation and diagnostics

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-VAL-001` | Readers shall report supported schema-profile violations with location where available. | Negative XML tests. | verified |
| `REQ-VAL-002` | Basic structural validation shall cover required fields, order, and cardinality for supported sequences. | Negative structural tests. | verified |
| `REQ-VAL-003` | Basic lexical validation shall cover common primitive simple types. | Simple type tests. | verified |

## Build and governance

| ID | Requirement | Verification | Status |
|---|---|---|---|
| `REQ-BUILD-001` | The build shall use Gradle Groovy DSL. | Build review. | verified |
| `REQ-BUILD-002` | The Java baseline shall be Java 21. | Toolchain config and CI. | verified |
| `REQ-BUILD-003` | Java 25 compatibility shall be tested in CI. | CI matrix. | verified |
| `REQ-QA-001` | The build shall include Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, and JaCoCo. | Build tasks and CI. | verified |
| `REQ-NI-001` | Generated sample bindings shall compile and execute under Native Image smoke tests. | Native workflow. | verified |
| `REQ-AGENT-001` | Agents shall operate only through approved task cards and file scopes. | Task-card and handoff review. | verified |
