package io.github.mundanej.mxjb.runtime;

import java.io.Serializable;
import java.util.Objects;

/** Best-effort source location for XML input or output diagnostics. */
public record XmlLocation(String systemId, int lineNumber, int columnNumber)
    implements Serializable {
  public static final XmlLocation UNKNOWN = new XmlLocation("", -1, -1);

  public XmlLocation {
    Objects.requireNonNull(systemId, "systemId");
    validateCoordinate("lineNumber", lineNumber);
    validateCoordinate("columnNumber", columnNumber);
  }

  private static void validateCoordinate(String name, int value) {
    if (value == 0 || value < -1) {
      throw new IllegalArgumentException(name + " must be -1 for unknown or greater than zero");
    }
  }
}
