package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** A raw, lexical XSD syntax node before component-graph resolution. */
public record XsdSyntaxNode(
    XsdSyntaxKind kind, Map<String, String> attributes, List<XsdSyntaxNode> children) {
  public XsdSyntaxNode {
    Objects.requireNonNull(kind, "kind");
    attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    children = List.copyOf(children);
  }

  String toText(String indent) {
    String line = indent + kind.manifestName() + attributesText();
    if (children.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + children.stream()
            .map(child -> child.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
  }

  private String attributesText() {
    if (attributes.isEmpty()) {
      return "";
    }
    return " "
        + attributes.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(" "));
  }
}
