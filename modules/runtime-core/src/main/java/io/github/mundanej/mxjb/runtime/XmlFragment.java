package io.github.mundanej.mxjb.runtime;

import java.util.List;
import java.util.Objects;

/** Dependency-free retained XML element fragment for accepted wildcard content. */
public record XmlFragment(
    XmlName name, List<XmlAttribute> attributes, List<XmlFragmentContent> content) {
  public XmlFragment {
    Objects.requireNonNull(name, "name");
    attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
    content = List.copyOf(Objects.requireNonNull(content, "content"));
  }
}
