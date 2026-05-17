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

Profile composition, full simple type semantics beyond `XP-VALIDATION-10-BASIC`, derivation,
substitution groups, mixed content, wildcards, identity constraints, nillable/default/fixed values,
XSD 1.1 assertions, streaming optimization, canonicalization, interop hardening, performance
tuning, and release engineering.
