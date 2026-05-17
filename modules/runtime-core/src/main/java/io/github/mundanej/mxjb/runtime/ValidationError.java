package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Generated validation failure value. */
public record ValidationError(String code, String message, XmlLocation location) {
  public ValidationError {
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
