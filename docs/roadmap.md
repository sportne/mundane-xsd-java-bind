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
Likely candidates include full simple type semantics beyond `XP-VALIDATION-10-BASIC`, full
derivation semantics, broader substitution-group behavior, wildcard and mixed-content shapes beyond
the accepted `XP-XSD10-DOCUMENT` subset, identity constraints, XSD 1.1 assertions, XML 1.1
compatibility, streaming optimization, broader external-suite intake, benchmark thresholds, and a
real signed publication workflow.

The project does not currently claim full XSD 1.0 conformance, XSD 1.1 support, W3C XML
Canonicalization, XML Signature canonical forms, lexical prefix preservation, hard performance
guarantees, artifact publication, or a release tag.
