# Project setup and build infrastructure

This page is the quick orientation guide for contributors who need to understand how the project is built. The deeper infrastructure policy lives in `docs/infrastructure/build-plan.md`, `docs/infrastructure/offline-build-plan.md`, and the build-related ADRs.

## Main tools

- Gradle 9.5.1 is the pinned build tool.
- The build uses the Groovy DSL, so build files end in `.gradle`.
- Java compilation targets Java 21 by default through Gradle toolchains.
- CI also keeps a Java 25 compatibility lane.
- Shared dependency and plugin versions live in `gradle/libs.versions.toml`.

See `docs/build/toolchain-matrix.md` for the JVM and Native Image lanes.

## Project layout

- `settings.gradle` lists every Gradle project and centralizes repositories.
- `build.gradle` at the root defines whole-repository tasks such as `qualityGate`.
- `build-logic/` is an included Gradle build that holds convention plugins.
- `modules/` contains publishable libraries, BOMs, and internal test modules.
- `examples/` contains non-published sample builds.
- `tools/` contains helper scripts for offline repository preparation and offline verification.

## Root tasks

- `./gradlew projects` shows the project tree.
- `./gradlew validateDesignControlPack` checks required governance and design docs.
- `./gradlew checkAll` runs each included project's `check` task.
- `./gradlew qualityGate` runs the normal local and CI gate.
- `./gradlew printPublishedArtifacts` prints the planned Maven coordinates.
- `./gradlew printOfflineBuildInstructions` prints the offline build command pattern.

`designControlStatus` is kept as an older compatibility alias for `validateDesignControlPack`.

## Convention plugins

Most module build files stay short because they apply convention plugins from `build-logic/src/main/groovy`.

- `mxjb.identity-conventions` sets group/version inheritance, reproducible archives, and dependency locking.
- `mxjb.java-conventions` sets Java 21 toolchains, JUnit, Error Prone, UTF-8, and test defaults.
- `mxjb.quality-conventions` configures Spotless, Checkstyle, and SpotBugs.
- `mxjb.coverage-conventions` configures JaCoCo reports and module-specific coverage verification for implemented modules.
- `mxjb.publishing-conventions` configures Maven publication for Java libraries.
- `mxjb.platform-conventions` configures Java platform BOM publication.
- `mxjb.application-conventions` configures application modules.
- `mxjb.native-conventions` configures GraalVM Native Image checks.
- `mxjb.docs-validation-conventions` adds design-control validation tasks.
- `mxjb.offline-conventions` adds offline build instructions.

## Build properties

- `mxjb.version` controls the Maven version for published artifacts.
- `mxjb.javaRelease` controls the Java release used by compile tasks.
- `mxjb.offlineRepo` points Gradle at a prepared local Maven repository for offline builds.
- `org.gradle.dependency.verification=strict` requires dependency verification metadata to match.

## Dependency policy

Dependency versions are locked with Gradle lockfiles. Dependency verification metadata lives in `gradle/verification-metadata.xml`. When dependencies or plugins change, update both the locks and verification metadata deliberately and review the diff.

Dependabot updates are grouped into monthly batch PRs on the 15th with a 90-day cooldown.

## Offline builds

Use `tools/prepare-offline-repository.sh` to prepare a local Maven repository, then verify it with `tools/verify-offline-build.sh`. See `docs/build/offline-build.md` for the contributor workflow.
