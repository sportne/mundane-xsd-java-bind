package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** XML Schema anyURI value retained without lossy java.net.URI normalization. */
public record XmlAnyUri(String lexicalValue) {
  public XmlAnyUri {
    Objects.requireNonNull(lexicalValue, "lexicalValue");
  }

  @Override
  public String toString() {
    return lexicalValue;
  }
}
