# Toolchain Matrix

The source and target Java baseline is Java 21.

| Lane | Purpose |
|---|---|
| Temurin 21 | Main contributor and CI lane. |
| Temurin 25 | Forward-compatibility JVM lane. |
| GraalVM 21 | Native Image and GraalVM JVM smoke lane. |
| GraalVM 25 | Forward-compatibility Native Image lane when available. |

Gradle Java toolchains select Java 21 for compilation. CI matrix jobs may run the same build under newer JVMs to catch runtime and tool compatibility issues.
