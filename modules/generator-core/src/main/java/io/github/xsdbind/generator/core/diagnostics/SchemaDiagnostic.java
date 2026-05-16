package io.github.xsdbind.generator.core.diagnostics;

import java.util.Objects;

/** A deterministic diagnostic emitted by the schema compiler. */
public record SchemaDiagnostic(DiagnosticCode code, String resource, String message) {
  public SchemaDiagnostic {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(message, "message");
  }

  public String toManifestLine() {
    return code + " | " + resource + " | " + message;
  }
}
