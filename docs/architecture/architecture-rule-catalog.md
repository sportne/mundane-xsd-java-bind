# Architecture rule catalog

This catalog records architecture rules that should be mechanically enforced with ArchUnit where
practical. Rules apply to production main code unless an entry says otherwise.

## Project-specific rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit evidence |
|---|---|---|---|---|
| Runtime modules must not depend on generator, CLI, Gradle plugin, examples, conformance, or testing-support packages. | Runtime and generated-code paths must stay small, stable, and reusable. | `runtime-core`, `runtime-jdkxml` main code. | None without ADR. | Runtime architecture tests. |
| Generated-code-facing modules must not depend on XML parser APIs. | Generated code talks to `runtime-core` abstractions, not parser implementations. | `runtime-core` and generated source fixtures. | `runtime-jdkxml` may use JDK XML/StAX APIs as the optional adapter. | Runtime architecture tests and emitter source tests. |
| `generator-api` must not expose parser, resolver, IR, binding, emitter, runtime, or entrypoint implementation packages. | The public API stays schema-to-Java only and does not freeze internals. | `generator-api` main code. | None without ADR. | `GeneratorApiArchitectureTest`. |
| Generator entrypoints may depend on generator API/core; runtime paths may not depend on CLI or Gradle plugin packages. | Entry points orchestrate generation, but generated/runtime artifacts must remain independent. | Runtime modules and `generator-cli` main code. | Future Gradle plugin code is approved only inside the Gradle plugin module. | Runtime and CLI architecture tests. |
| Generated source must remain free of binding annotations, reflection, discovery, classpath scanning, generator packages, and parser APIs. | Generated bindings must be explicit, readable, deterministic, and Native Image friendly. | Generated source emitted in generator-core tests. | Test harness code may use reflection/classloaders to compile and execute generated source. | Generated model/reader/writer/validator emitter tests. |

## GraalVM Native Image rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit evidence |
|---|---|---|---|---|
| Runtime and generated-code paths must not use reflection, `java.lang.invoke`, dynamic proxies, `ServiceLoader`, `ClassLoader`, or classpath-scanning libraries. | These mechanisms commonly require reachability metadata or dynamic discovery that hides architecture drift. | Runtime modules, generator API, CLI, and generator pipeline packages that produce generated/runtime behavior. | Test harnesses only; ADR required for production. | Module architecture tests and generated-source token checks. |
| Runtime and generated-code paths must not use Java serialization mechanisms. | Java serialization is metadata-heavy, fragile across native images, and unnecessary for generated XML binding. | Runtime modules, generator API, CLI, and generator pipeline packages. | `XmlLocation` and `XmlDiagnostic` currently keep explicit `Serializable` value compatibility; Java records may also expose implicit JDK `Serializable` bytecode shape. Serialization streams, `Externalizable`, generated `Serializable`, and serialization hooks remain forbidden. | Module architecture tests and generated-source token checks. |
| Runtime and generated-code paths must not use internal JDK APIs, `Unsafe`, finalizers, forced GC, or security-manager-era APIs. | These APIs are brittle across JDKs and Native Image configurations. | Runtime modules, generator API, CLI, and generator-core main code. | None without ADR. | Module architecture tests. |
| Resource access must be explicit and bounded. | Native images need intentional resource reachability; implicit classpath lookup should not become binding behavior. | Production runtime/generated paths. | Test harnesses may load test resources; optional adapters may declare explicit resource policy if later needed. | Existing `ClassLoader` bans and source checks. |

## General Java baseline rules

| Rule | Rationale | Enforced scope | Allowed exceptions | ArchUnit evidence |
|---|---|---|---|---|
| No `ObjectInputStream`, `ObjectOutputStream`, new explicit `Serializable`, or `Externalizable` in main code. | Serialization should be explicitly justified and is not needed for current module responsibilities. | Production modules with Java main code. | Existing explicit `Serializable` is limited to `runtime-core` diagnostic value records `XmlLocation` and `XmlDiagnostic`; new explicit serialization constructs require ADR. | Module architecture tests and generated-source token checks. |
| No finalizers. | Finalization is deprecated, unpredictable, and hostile to deterministic resource management. | Production modules with Java main code. | None. | Module architecture tests. |
| No direct `System.exit` outside CLI entrypoint code. | Libraries must return diagnostics/errors instead of terminating the host process. | Production modules; `generator-cli` allows `MxjbCli.main`. | CLI entrypoint only. | Module architecture tests. |
| No `Runtime`/`ProcessBuilder` process spawning outside approved tooling. | Process execution is a security and portability boundary. | Production modules. | Future build/tooling integrations require a task and docs. | Module architecture tests. |
| No public static mutable fields. | Global mutable state makes behavior order-dependent and hard to reason about. | Production modules with Java main code. | None without ADR. | Module architecture tests. |
| No direct use of internal JDK packages. | Internal APIs are not stable across Java releases. | Production modules with Java main code. | None without ADR. | Module architecture tests. |
