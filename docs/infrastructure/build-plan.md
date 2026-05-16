# Build and infrastructure plan

Contributor-facing setup instructions live in `docs/build/README.md`. This file records the infrastructure shape the project is expected to keep.

## Baseline

- Java 21 source/release baseline.
- Java 25 compatibility lane.
- Gradle 9.5.1 Groovy DSL.
- Multi-project build.
- Version catalog.
- Dependency verification metadata.
- Dependency locking.
- Composable convention plugins in `build-logic`.
- Checkstyle, Spotless, SpotBugs, Error Prone.
- JUnit Platform/Jupiter.
- ArchUnit.
- JaCoCo.
- GraalVM Native Image build tools.

## Repository layout

- Root `settings.gradle` includes `build-logic` through `pluginManagement`.
- Root `settings.gradle` centralizes repositories and uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Published libraries, the BOM, and internal test modules live under `modules/`.
- Non-published examples live under top-level `examples/`.
- Shared build behavior lives in composable convention plugins under `build-logic/src/main/groovy`.
- Helper scripts live under `tools/`.

## Public Gradle interface

- `validateDesignControlPack` verifies required design-control files.
- `designControlStatus` remains as a compatibility alias.
- `checkAll` runs checks for every included project.
- `qualityGate` is the local and CI quality gate.
- `printPublishedArtifacts` prints planned Maven coordinates.
- `printOfflineBuildInstructions` explains the offline build path.

## Build properties

- `xsdbind.version` controls published artifact versions.
- `xsdbind.javaRelease` controls Java compile release and toolchain selection.
- `xsdbind.offlineRepo` selects a local Maven repository for offline dependency resolution.

## Build phases

1. `help` and `projects` must work after wrapper/dependency hydration.
2. `validateDesignControlPack` verifies required design-control files.
3. `qualityGate` runs static analysis, formatting checks, coverage verification, and tests once source exists.
4. `nativeTest` is enabled for sample/generated-code modules when phase-one generated bindings exist.

## Source layout policy

No product implementation source may be added before the design-control gate is accepted. Placeholder `.gitkeep` files are allowed.

## Dependency and update policy

Dependency verification runs in strict mode. Dependency lockfiles and `gradle/verification-metadata.xml` must be updated together when build dependencies change.

Dependabot updates are grouped into monthly batch PRs on the 15th with a 90-day cooldown.
