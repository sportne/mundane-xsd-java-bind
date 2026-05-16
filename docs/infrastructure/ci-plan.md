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

Native Image CI is allowed to be placeholder-only until generated binding samples exist, then it becomes required for phase-one release candidates.
