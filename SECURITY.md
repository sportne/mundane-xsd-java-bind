# Security

`xsd-bind-java` treats XML resource handling, generated-code determinism, and runtime dependency control as security-sensitive project areas.

## Reporting

Report suspected vulnerabilities through the repository security advisory flow when available, or through the maintainer contact configured for the GitHub repository. Do not open public issues for exploitable XML parsing, resource resolution, generated-code injection, or build supply-chain findings.

## Security-sensitive changes

Changes in these areas require explicit review against `AGENT.md`, `docs/architecture/security-architecture.md`, and the ADR index:

- XML resource resolution and catalog behavior.
- Generated source emission.
- Runtime dependencies.
- Reflection, classpath scanning, dynamic proxies, and service discovery.
- Dependency verification, dependency locking, wrapper metadata, and offline build support.
