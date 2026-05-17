package io.github.mundanej.mxjb.generator.api;

import java.util.Objects;

/** Public deterministic generator diagnostic. */
public record GeneratorDiagnostic(String code, String resource, String message) {
  public GeneratorDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(message, "message");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }

  public String toManifestLine() {
    return code + " | " + resource + " | " + message;
  }
}
