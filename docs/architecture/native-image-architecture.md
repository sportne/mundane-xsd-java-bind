# Native Image architecture

## Policy

Native Image compatibility is a first-class acceptance criterion, not a release afterthought.

## Rules

- Generated model and codec code must not use reflection.
- Runtime core must not use classpath scanning, dynamic proxies, or runtime binding discovery.
- Resource access must be explicit and bounded.
- Optional adapters that need metadata must declare it and remain outside generated-runtime requirements.
- CI must include Native Image smoke tests once generated sample bindings exist.

## Native test stages

1. Infrastructure smoke: native plugin task wiring exists.
2. Generated sample smoke: compile one generated binding to a native executable.
3. Round-trip native tests: execute representative XML read/write tests in native mode.
4. Conformance subset native tests: run selected conformance cases under native execution.
