package io.github.mundanej.mxjb.runtime;

import java.io.Serializable;
import java.util.Objects;

/** Stable XML runtime diagnostic value. */
public record XmlDiagnostic(
    XmlDiagnosticSeverity severity, String code, String message, XmlLocation location)
    implements Serializable {
  public XmlDiagnostic {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(location, "location");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }
}
