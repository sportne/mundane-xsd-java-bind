package com.example.document;

import io.github.mundanej.mxjb.runtime.XmlFragment;
import java.util.Objects;

/** Generated branch for XML mixed content wildcard. */
public record OrderWildcardContent(XmlFragment value) implements OrderContent {
  public OrderWildcardContent {
    Objects.requireNonNull(value, "value");
  }
}
