# CI plan

## Workflows

| Workflow | Purpose |
|---|---|
| `ci.yml` | Java 21 and Java 25 build/check matrix. |
| `native-image.yml` | GraalVM Native Image lane. |
| `docs.yml` | Documentation and design-control checks. |
| `nightly.yml` | Scheduled clean quality-gate run. |

## Required CI stages

1. Checkout.
2. Setup JDK.
3. Setup Gradle cache.
4. Run `./gradlew qualityGate` in the main CI matrix.
5. Run `./gradlew validateDesignControlPack` in documentation and native placeholder lanes.
6. Upload reports on failure once product test reports exist.

## Native Image lane

Native Image CI is allowed to be placeholder-only until runtime or generated-code tasks create meaningful executable native tests. Runtime primitive native tests start with `TASK-0010` when runtime-core behavior exists, generated-code smoke starts with `TASK-0013` when generated code can execute with runtime primitives, and generated binding round-trip/native sample checks become required before phase-one release candidates.
