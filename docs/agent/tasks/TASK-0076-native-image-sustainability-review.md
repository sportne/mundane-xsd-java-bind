# TASK-0076: native-image-sustainability-review

Status: draft.

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

