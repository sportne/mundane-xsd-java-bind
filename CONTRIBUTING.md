# Contributing

This repository is under design control. Contributors and coding agents must follow `AGENT.md`, the ADR process, and the requirements taxonomy.

## Contribution order

1. Review the current task gate in `docs/agent/handoff.md`.
2. Confirm the relevant task card is accepted or explicitly promoted for implementation.
3. Keep changes inside the allowed scope for that task card.
4. Update requirements, conformance, verification, and handoff docs when behavior or evidence changes.
5. Run the documented checks for the task before asking for review.

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

When a change touches generated-code execution, examples, native compatibility, or release
readiness evidence, also run the documented `nativeSmoke` command with GraalVM available.

Use `./gradlew projects` to orient yourself in the multi-project build. Shared build conventions are in `build-logic/`; module build files should stay small and apply those conventions instead of duplicating tool configuration.

## Forbidden without ADR approval

- runtime third-party dependencies
- reflection-based XML binding
- annotation-driven runtime binding discovery
- ServiceLoader or classpath scanning in generated-runtime paths
- expansion of supported XML Schema profile without conformance matrix update
- weakening quality, coverage, architecture, or security gates
