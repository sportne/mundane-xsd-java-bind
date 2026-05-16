# Traceability matrix

| Requirement | ADR | Architecture | Verification | Status |
|---|---|---|---|---|
| `REQ-SCOPE-001` | `ADR-0002` | `scope-and-non-goals.md` | CLI/API absence tests | accepted |
| `REQ-SCOPE-002` | `ADR-0002` | `scope-and-non-goals.md` | Build and API surface review | accepted |
| `REQ-RT-001` | `ADR-0003` | `module-boundaries.md` | Dependency and ArchUnit tests | accepted |
| `REQ-RT-002` | `ADR-0004` | `generated-code-contract.md` | Source inspection, ArchUnit, Native Image tests | accepted |
| `REQ-SCHEMA-001` | `ADR-0006` | `compiler-pipeline.md` | `TASK-0007` frontend syntax tests; `TASK-0008` normalized IR tests; `TASK-0009` binding root/field tests; later round-trip tests | accepted |
| `REQ-SCHEMA-002` | `ADR-0006` | `compiler-pipeline.md`, `generated-code-contract.md` | `TASK-0007` frontend syntax tests; `TASK-0008` normalized IR tests; `TASK-0009` complex-type binding tests; later generated compile tests | accepted |
| `REQ-SCHEMA-003` | `ADR-0006` | `compiler-pipeline.md`, `validation-architecture.md` | `TASK-0007` sequence syntax tests; `TASK-0008` sequence IR tests; `TASK-0009` sequence metadata tests; later valid/invalid sequence tests | accepted |
| `REQ-SCHEMA-004` | `ADR-0006`, `ADR-0007` | `compiler-pipeline.md`, `validation-architecture.md` | `TASK-0007` cardinality syntax tests; `TASK-0008` cardinality IR tests; `TASK-0009` field cardinality tests; later cardinality behavior tests | accepted |
| `REQ-SCHEMA-005` | `ADR-0014` | `compiler-pipeline.md`, `security-architecture.md` | `TASK-0006` resolver tests with offline fixtures | accepted |
| `REQ-SCHEMA-006` | `ADR-0009` | `compiler-pipeline.md`, `module-boundaries.md` | `TASK-0007` namespace syntax tests; `TASK-0008` QName/namespace IR tests; `TASK-0009` package mapping tests; later multi-namespace examples | accepted |
| `REQ-SCHEMA-007` | `ADR-0006`, `ADR-0008` | `generated-code-contract.md`, `conformance/matrix.md` | `TASK-0007` profile-gated unsupported diagnostics until approved | deferred |
| `REQ-RES-001` | `ADR-0014` | `security-architecture.md`, `compiler-pipeline.md` | `TASK-0006` local resolver and catalog tests | accepted |
| `REQ-SEC-001` | `ADR-0014` | `security-architecture.md` | `TASK-0006` denied-network and cycle tests | accepted |
| `REQ-NS-001` | `ADR-0009` | `compiler-pipeline.md`, `generated-code-contract.md` | `TASK-0008` QName/namespace IR tests; `TASK-0009` namespace-to-package binding tests; later public binding config tests | accepted |
| `REQ-GEN-001` | `ADR-0008` | `compiler-pipeline.md` | `TASK-0009` deterministic binding tests; later golden source tests | accepted |
| `REQ-GEN-002` | `ADR-0008`, `ADR-0011` | `generated-code-contract.md`, `build-plan.md` | Compile and static-analysis tests | accepted |
| `REQ-MODEL-001` | `ADR-0008` | `generated-code-contract.md` | `TASK-0009` record-candidate binding tests; later source and behavior tests | accepted |
| `REQ-XML-W-001` | `ADR-0005`, `ADR-0009` | `runtime-architecture.md`, `generated-code-contract.md` | XML output tests | accepted |
| `REQ-XML-R-001` | `ADR-0005`, `ADR-0007` | `runtime-architecture.md`, `validation-architecture.md` | XML input tests | accepted |
| `REQ-VAL-001` | `ADR-0007` | `validation-architecture.md` | Negative XML diagnostic tests | accepted |
| `REQ-VAL-002` | `ADR-0007` | `validation-architecture.md` | `TASK-0009` validation-plan metadata tests; later negative structural tests | accepted |
| `REQ-VAL-003` | `ADR-0007` | `validation-architecture.md` | Simple type tests | accepted |
| `REQ-BUILD-001` | `ADR-0011` | `build-plan.md` | Gradle task review | accepted |
| `REQ-BUILD-002` | `ADR-0011` | `build-plan.md`, `toolchain-matrix.md` | Toolchain config and CI | accepted |
| `REQ-BUILD-003` | `ADR-0011` | `toolchain-matrix.md` | CI Java 25 lane | accepted |
| `REQ-QA-001` | `ADR-0011` | `build-plan.md` | Quality gate tasks and CI | accepted |
| `REQ-NI-001` | `ADR-0010` | `native-image-architecture.md`, `native-image-test-plan.md` | Native workflow and smoke tests beginning with the first executable runtime/generated-code surfaces (`TASK-0010`, `TASK-0013`, `TASK-0017`) and hardening in `TASK-0020`/`TASK-0044` | accepted |
| `REQ-AGENT-001` | `ADR-0013` | `AGENT.md`, `docs/agent/handoff.md` | Task-card review | accepted |
