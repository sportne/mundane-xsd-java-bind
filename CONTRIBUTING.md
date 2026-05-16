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

## Forbidden without ADR approval

- runtime third-party dependencies
- reflection-based XML binding
- annotation-driven runtime binding discovery
- ServiceLoader or classpath scanning in generated-runtime paths
- expansion of supported XML Schema profile without conformance matrix update
- weakening quality, coverage, architecture, or security gates
