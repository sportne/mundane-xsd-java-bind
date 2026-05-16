# Gradle Wrapper

`gradle-wrapper.properties` pins Gradle 9.5.1. The POSIX `gradlew` script bootstraps `gradle-wrapper.jar` from Gradle's distribution service on first use and verifies the published SHA-256 when `sha256sum` is available.

For strict offline repositories, place the verified `gradle-wrapper.jar` here before invoking `./gradlew --offline`.
