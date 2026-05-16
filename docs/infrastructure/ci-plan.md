# CI plan

## Workflows

| Workflow | Purpose |
|---|---|
| `ci.yml` | Java 21 and Java 25 build/check matrix. |
| `native-image.yml` | GraalVM Native Image lane. |
| `docs.yml` | Documentation and design-control checks. |

## Required CI stages

1. Checkout.
2. Setup JDK.
3. Setup Gradle cache.
4. Run `./gradlew designControlStatus`.
5. Run `./gradlew check` when source and dependencies are hydrated.
6. Upload reports on failure.

## Native Image lane

Native Image CI is allowed to be manual or scheduled until generated binding samples exist, then it becomes required for phase-one release candidates.
