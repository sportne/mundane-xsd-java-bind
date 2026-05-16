# Verification and validation plan

## Test layers

| Layer | Purpose |
|---|---|
| Unit tests | Parser/model/binding/code-emitter/runtime primitives. |
| Golden source tests | Generated source must match approved output exactly. |
| Compile tests | Generated source compiles under Java 21 and Java 25 lanes. |
| Round-trip tests | Object → XML → object and XML → object → XML. |
| Negative tests | Invalid XML/schema/profile inputs produce deterministic diagnostics. |
| Differential tests | Compare selected behavior with JDK XML validation/tooling where useful. |
| Conformance harness | Run selected W3C XML/XSD tests by profile. |
| Architecture tests | Enforce module boundaries and forbidden runtime behavior. |
| Static analysis | Checkstyle, Spotless, SpotBugs, Error Prone. |
| Coverage gates | JaCoCo aggregate and per-file thresholds. |
| Native tests | Compile and execute runtime primitives and generated sample bindings as native images when each surface becomes executable. |
| Security tests | XXE, entity expansion, resolver denial, excessive nesting. |
| Documentation tests | Verify examples, commands, requirement and ADR trace links. |

## Phase-one verification minimum

- XSD fixture → binding IR golden test.
- XSD fixture → generated source golden test.
- Generated source compile test.
- XML input → object test.
- Object → XML output test.
- Round-trip test.
- Negative XML diagnostic test.
- Basic Native Image smoke test.
