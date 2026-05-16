# Native Image test plan

## Purpose

Native Image tests prove that generated bindings and runtime paths avoid unexpected dynamic JVM behavior.

## Stages

1. **Wiring stage:** native plugin configured; no product tests yet.
2. **Smoke stage:** generated sample binding compiles and runs as native executable.
3. **Round-trip stage:** generated XML read/write test runs in native executable.
4. **Conformance stage:** selected profile tests run in native executable.

## Failure policy

A Native Image failure caused by reflection, resource lookup, proxy generation, serialization metadata, or classpath scanning must be treated as an architecture issue unless explicitly approved by ADR.
