# Contributing

This repository is under design control. Contributors and coding agents must follow `AGENT.md`, the ADR process, and the requirements taxonomy.

## Contribution order

1. Design-control pack review.
2. Build scaffold validation.
3. Quality gate hardening.
4. Phase-one design readiness review.
5. Only then: schema compiler/runtime implementation.

## Pull request requirements

Every pull request must include:

- requirement IDs affected
- ADR impact statement
- changed files summary
- tests added or updated
- commands run
- Native Image impact assessment
- runtime dependency impact assessment

## Local checks

Before opening a pull request, run:

```bash
./gradlew validateDesignControlPack
./gradlew qualityGate
```

Use `./gradlew projects` to orient yourself in the multi-project build. Shared build conventions are in `build-logic/`; module build files should stay small and apply those conventions instead of duplicating tool configuration.

## Forbidden without ADR approval

- runtime third-party dependencies
- reflection-based XML binding
- annotation-driven runtime binding discovery
- ServiceLoader or classpath scanning in generated-runtime paths
- expansion of supported XML Schema profile without conformance matrix update
- weakening quality, coverage, architecture, or security gates
