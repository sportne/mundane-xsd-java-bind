package io.github.mundanej.mxjb.runtime;

import java.util.Objects;

/** Retained text content inside an unknown XML fragment. */
public record XmlFragmentText(String text) implements XmlFragmentContent {
  public XmlFragmentText {
    Objects.requireNonNull(text, "text");
  }
}
