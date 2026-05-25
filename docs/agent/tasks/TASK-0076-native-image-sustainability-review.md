# TASK-0076: native-image-sustainability-review

Status: accepted.

Task ID: `TASK-0076`
Priority: P2
Gate: GraalVM Native Image sustainability review.
Target areas: nativeSmoke, nativeConformance, generated-code native smoke, resource inclusion,
reflection/proxy absence, runtime-jdkxml adapters, CI/native docs, and Native Image architecture.
Allowed files: native build tasks, native tests, docs, and narrow resource/config fixes for existing
lanes.
Forbidden files: reflection-based generated code, broad runtime dependencies, release metadata,
publication changes, or quality-gate weakening.
Expected behavior: explain and verify why generated bindings remain Native Image friendly after
`XP-XSD10-FULL`. Review resource flags, JDK XML parser resource bundles, no-reflection guarantees,
generated sealed/record usage, conformance executable size, CI toolchain behavior, and known
GraalVM warnings.
Tests to add/update: native lane assertions where practical, static generated-source checks for
forbidden mechanisms, and docs for required SDKMAN/GraalVM setup.
Commands to run: `bash -lc 'source "$HOME/.sdkman/bin/sdkman-init.sh" && ./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain'`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: native compatibility is still demonstrable and documented; experimental Native
Image flags are understood; no reflection/proxy/resource surprises are hidden.
Rollback notes: revert native/docs changes.

## Completion notes

`TASK-0076` adds `docs/verification/native-image-sustainability-review.md` and updates Native Image
setup documentation to use SDKMAN GraalVM:
`source "$HOME/.sdkman/bin/sdkman-init.sh"` before running Gradle native lanes.

The review confirms the post-`XP-XSD10-FULL` native posture remains static-source friendly:
generated records, sealed branch interfaces, readers, writers, and validators do not require
reflection/proxy/classpath scanning; runtime-core and runtime-jdkxml are exercised by native tests;
example bindings and selected conformance execute generated read/write/validate paths under Native
Image. The known experimental Native Image resource flags are documented as fixture/JDK XML message
resource flags for test/conformance executables, not product runtime dependencies.

No native build task changes, reflection-based generated code, broad runtime dependency, release
metadata, publication behavior, or quality-gate weakening is introduced.

## Evidence

- `bash -lc 'source "$HOME/.sdkman/bin/sdkman-init.sh" && ./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain'`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
