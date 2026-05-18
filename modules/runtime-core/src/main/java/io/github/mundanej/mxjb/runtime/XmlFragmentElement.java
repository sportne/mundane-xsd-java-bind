package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Retained child element content inside an unknown XML fragment. */
public record XmlFragmentElement(XmlFragment fragment) implements XmlFragmentContent {
  public XmlFragmentElement {
    Objects.requireNonNull(fragment, "fragment");
  }
}
