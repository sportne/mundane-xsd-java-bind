# Offline build plan

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
./gradlew --offline -Pxsdbind.offlineRepo=.repo/offline-maven check
```

## Agent rule

An agent may not introduce hidden network access. Any remote resource use must be explicit, documented, and test-tagged as integration-only.

## Dependency verification bootstrap

Dependency verification starts in lenient mode in `gradle.properties` so the v0.1 scaffold can hydrate. `TASK-0002` must generate and review full verification metadata, then switch CI to strict mode.
