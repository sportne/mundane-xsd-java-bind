# Offline build plan

Contributor-facing commands live in `docs/build/offline-build.md`.

## Goal

Support builds with no remote repository access after a local Maven repository and Gradle wrapper have been hydrated.

## Offline inputs

- `gradle/wrapper/gradle-wrapper.jar`
- Gradle distribution already present in Gradle user home, or an internal mirror configured in `gradle-wrapper.properties`
- local Maven repository path, for example `.repo/offline-maven`
- Gradle dependency verification metadata
- dependency locks after first online hydration

## Offline invocation

```bash
./gradlew --offline -Pmxjb.offlineRepo=.repo/offline-maven qualityGate
```

## Agent rule

An agent may not introduce hidden network access. Any remote resource use must be explicit, documented, and test-tagged as integration-only.

## Dependency verification bootstrap

Dependency verification runs in strict mode once metadata is hydrated. `TASK-0002` must keep verification metadata and dependency locks current whenever build dependencies change.
