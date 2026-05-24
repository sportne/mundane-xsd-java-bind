# Roadmap

## Phase 0: Design-control baseline

Establish charter, scope, standards, profiles, requirements, architecture, verification, build plan, ADRs, and agent governance.

## Phase 1: Infrastructure scaffold

Make the repository enforce discipline before product code exists.

## Phase 2: Schema compiler vertical slice

Parse and normalize the first XSD data-structure subset.

## Phase 3: Generated model and writer vertical slice

Generate Java model classes/records and XML writers.

## Phase 4: Generated reader and basic validation vertical slice

Generate XML readers and basic structural/lexical validation.

## Phase 5: First complete public vertical slice

Deliver CLI and Gradle plugin generation for supported data-structure schemas with round-trip and Native Image smoke tests.

## Future phases

Future phases must be opened through new task gates after the accepted `0.6.0` hardening closeout.
The current product program is `XP-XSD10-FULL`: complete XML Schema 1.0 support for the binding
generator through the task sequence that began at `TASK-0048`, starting from the feature matrix in
`docs/verification/xsd10-full-feature-matrix.md`. `TASK-0056` closes the current sequence without
enabling the full profile because the feature matrix and W3C suite evidence still show blockers.
Other candidates include a new full-XSD gap-closure sequence, streaming optimization, broader
external-suite automation, benchmark thresholds, and a real signed publication workflow.

The project does not currently claim full XSD 1.0 conformance, XSD 1.1 support, XML 1.1 support,
W3C XML Canonicalization, XML Signature canonical forms, lexical prefix preservation, hard
performance guarantees, artifact publication, or a release tag.
