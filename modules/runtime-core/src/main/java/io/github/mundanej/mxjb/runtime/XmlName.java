package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** XML expanded name used by generated readers and writers. */
public record XmlName(String namespaceUri, String localName) {
  public XmlName {
    Objects.requireNonNull(namespaceUri, "namespaceUri");
    Objects.requireNonNull(localName, "localName");
    if (localName.isBlank()) {
      throw new IllegalArgumentException("localName must not be blank");
    }
  }
}
