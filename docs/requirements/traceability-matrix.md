# Traceability matrix

| Requirement | ADR | Architecture | Verification | Status |
|---|---|---|---|---|
| `REQ-SCOPE-001` | `ADR-0002` | `scope-and-non-goals.md` | CLI/API absence tests | accepted |
| `REQ-SCOPE-002` | `ADR-0002` | `scope-and-non-goals.md` | Build and API surface review | accepted |
| `REQ-RT-001` | `ADR-0003` | `module-boundaries.md` | `TASK-0010` runtime-core unit and ArchUnit tests; `TASK-0014` optional runtime-jdkxml adapter and module-boundary tests | accepted |
| `REQ-RT-002` | `ADR-0004` | `generated-code-contract.md` | Source inspection, ArchUnit, generated model/writer/reader source tests, `TASK-0013` generated-code Native Image smoke evidence | accepted |
| `REQ-SCHEMA-001` | `ADR-0006` | `compiler-pipeline.md` | `TASK-0007` frontend syntax tests; `TASK-0008` normalized IR tests; `TASK-0009` binding root/field tests; `TASK-0015` generated reader element tests; later round-trip tests | accepted |
| `REQ-SCHEMA-002` | `ADR-0006` | `compiler-pipeline.md`, `generated-code-contract.md` | `TASK-0007` frontend syntax tests; `TASK-0008` normalized IR tests; `TASK-0009` complex-type binding tests; `TASK-0015` nested generated reader tests; later generated compile tests | accepted |
| `REQ-SCHEMA-003` | `ADR-0006` | `compiler-pipeline.md`, `validation-architecture.md` | `TASK-0007` sequence syntax tests; `TASK-0008` sequence IR tests; `TASK-0009` sequence metadata tests; `TASK-0015` reader order tests; later valid/invalid sequence tests | accepted |
| `REQ-SCHEMA-004` | `ADR-0006`, `ADR-0007` | `compiler-pipeline.md`, `validation-architecture.md` | `TASK-0007` cardinality syntax tests; `TASK-0008` cardinality IR tests; `TASK-0009` field cardinality tests; `TASK-0015` reader missing/repeated cardinality diagnostics; later cardinality behavior tests | accepted |
| `REQ-SCHEMA-005` | `ADR-0014` | `compiler-pipeline.md`, `security-architecture.md` | `TASK-0006` resolver tests with offline fixtures | accepted |
| `REQ-SCHEMA-006` | `ADR-0009` | `compiler-pipeline.md`, `module-boundaries.md` | `TASK-0007` namespace syntax tests; `TASK-0008` QName/namespace IR tests; `TASK-0009` package mapping tests; `TASK-0015` reader namespace matching tests; later multi-namespace examples | accepted |
| `REQ-SCHEMA-007` | `ADR-0006`, `ADR-0008` | `generated-code-contract.md`, `conformance/matrix.md` | `TASK-0007` profile-gated unsupported diagnostics until approved | deferred |
| `REQ-RES-001` | `ADR-0014` | `security-architecture.md`, `compiler-pipeline.md` | `TASK-0006` local resolver and catalog tests | accepted |
| `REQ-SEC-001` | `ADR-0014` | `security-architecture.md` | `TASK-0006` denied-network and cycle tests; `TASK-0014` denied external XML resource/entity tests | accepted |
| `REQ-NS-001` | `ADR-0009` | `compiler-pipeline.md`, `generated-code-contract.md` | `TASK-0008` QName/namespace IR tests; `TASK-0009` namespace-to-package binding tests; `TASK-0011` package/path and cross-package model reference tests; `TASK-0012` writer `XmlName` source/behavior tests; `TASK-0015` reader `XmlName` source/behavior tests; later public binding config tests | accepted |
| `REQ-GEN-001` | `ADR-0008` | `compiler-pipeline.md` | `TASK-0009` deterministic binding tests; `TASK-0011` deterministic generated model source tests; `TASK-0012` deterministic generated writer source tests; `TASK-0013` reusable deterministic/golden harness tests; `TASK-0015` deterministic generated reader golden source tests | accepted |
| `REQ-GEN-002` | `ADR-0008`, `ADR-0011` | `generated-code-contract.md`, `build-plan.md` | `TASK-0011` generated model compile tests; `TASK-0012` generated writer compile/behavior tests; `TASK-0013` canonical generated-source compile/JVM smoke/native smoke harness; `TASK-0015` generated reader compile/behavior tests; generator-core static-analysis checks | accepted |
| `REQ-MODEL-001` | `ADR-0008` | `generated-code-contract.md` | `TASK-0009` record-candidate binding tests; `TASK-0011` generated record source and behavior tests; later source and behavior tests for non-record shapes | accepted |
| `REQ-XML-W-001` | `ADR-0005`, `ADR-0009` | `runtime-architecture.md`, `generated-code-contract.md` | `TASK-0010` `XmlOutput` interface tests; `TASK-0012` generated XML writer output tests; `TASK-0013` generated writer JVM/native smoke; `TASK-0014` JDK XML output adapter tests; later round-trip tests | accepted |
| `REQ-XML-R-001` | `ADR-0005`, `ADR-0007` | `runtime-architecture.md`, `validation-architecture.md` | `TASK-0010` `XmlEventReader` interface tests; `TASK-0014` JDK XML event reader adapter tests; `TASK-0015` generated XML input tests | accepted |
| `REQ-VAL-001` | `ADR-0007` | `validation-architecture.md` | `TASK-0010` runtime diagnostic and validation result tests; `TASK-0014` JDK XML diagnostic wrapping tests; `TASK-0015` reader diagnostic tests; later validation-result tests | accepted |
| `REQ-VAL-002` | `ADR-0007` | `validation-architecture.md` | `TASK-0009` validation-plan metadata tests; `TASK-0015` negative structural reader diagnostics; later generated validation tests | accepted |
| `REQ-VAL-003` | `ADR-0007` | `validation-architecture.md` | `TASK-0015` scalar lexical conversion tests; later facet validation tests | accepted |
| `REQ-BUILD-001` | `ADR-0011` | `build-plan.md` | Gradle task review | accepted |
| `REQ-BUILD-002` | `ADR-0011` | `build-plan.md`, `toolchain-matrix.md` | Toolchain config and CI | accepted |
| `REQ-BUILD-003` | `ADR-0011` | `toolchain-matrix.md` | CI Java 25 lane | accepted |
| `REQ-QA-001` | `ADR-0011` | `build-plan.md` | Quality gate tasks, generated-code smoke task, and CI | accepted |
| `REQ-NI-001` | `ADR-0010` | `native-image-architecture.md`, `docs/verification/native-image-test-plan.md` | Native workflow and smoke tests beginning with the first executable runtime/generated-code surfaces (`TASK-0010`, `TASK-0013`, `TASK-0017`) and hardening in `TASK-0020`/`TASK-0044`; `TASK-0010` records passing runtime primitive native smoke evidence; `TASK-0013` records passing generated-code native smoke evidence | accepted |
| `REQ-AGENT-001` | `ADR-0013` | `AGENT.md`, `docs/agent/handoff.md` | Task-card review | accepted |
