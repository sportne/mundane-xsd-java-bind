package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Immutable XML attribute retained inside an unknown XML fragment. */
public record XmlAttribute(XmlName name, String value) {
  public XmlAttribute {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
  }
}
