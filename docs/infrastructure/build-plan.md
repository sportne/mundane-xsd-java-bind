# Build and infrastructure plan

## Baseline

- Java 21 source/release baseline.
- Java 25 compatibility lane.
- Gradle 9.5.1 Groovy DSL.
- Multi-project build.
- Version catalog.
- Dependency verification metadata.
- Checkstyle, Spotless, SpotBugs, Error Prone.
- JUnit Platform/Jupiter.
- ArchUnit.
- JaCoCo.
- GraalVM Native Image build tools.

## Build phases

1. `help` and `projects` must work after wrapper/dependency hydration.
2. `designControlStatus` verifies required design-control files.
3. `check` runs static analysis and tests once source exists.
4. `nativeTest` is enabled for sample/generated-code modules when phase-one generated bindings exist.

## Source layout policy

No product implementation source may be added before the design-control gate is accepted. Placeholder `.gitkeep` files are allowed.
