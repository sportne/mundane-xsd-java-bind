# Native Image architecture

## Policy

Native Image compatibility is a first-class acceptance criterion, not a release afterthought.

## Rules

- Generated model and codec code must not use reflection.
- Runtime core must not use classpath scanning, dynamic proxies, or runtime binding discovery.
- Resource access must be explicit and bounded.
- Optional adapters that need metadata must declare it and remain outside generated-runtime requirements.
- Native Image checks must start as soon as there is meaningful executable runtime or generated binding behavior; generated sample bindings expand that lane rather than introduce it for the first time.

## Native test stages

1. Infrastructure smoke: native plugin task wiring exists.
2. Runtime primitive smoke: execute runtime-core primitive tests in native mode once runtime-core has executable behavior.
3. Generated sample smoke: compile one generated binding to a native executable.
4. Round-trip native tests: execute representative XML read/write tests in native mode.
5. Conformance subset native tests: run selected conformance cases under native execution.
