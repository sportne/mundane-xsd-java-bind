package io.github.xsdbind.generator.core.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Raw syntax captured from one resolver-approved XSD document. */
public record XsdSyntaxDocument(
    String resourceId,
    String targetNamespace,
    Map<String, String> namespaceDeclarations,
    List<XsdSyntaxNode> children) {
  public XsdSyntaxDocument {
    Objects.requireNonNull(resourceId, "resourceId");
    targetNamespace = targetNamespace == null ? "" : targetNamespace;
    namespaceDeclarations = Collections.unmodifiableMap(new LinkedHashMap<>(namespaceDeclarations));
    children = List.copyOf(children);
  }

  public String toText() {
    String documentLine = "document " + resourceId + " namespace=" + targetNamespace;
    String namespaceText =
        namespaceDeclarations.entrySet().stream()
            .map(entry -> "  namespace " + entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("\n"));
    String childText =
        children.stream().map(child -> child.toText("  ")).collect(Collectors.joining("\n"));
    return joinNonEmpty(documentLine, namespaceText, childText);
  }

  private String joinNonEmpty(String first, String second, String third) {
    return java.util.stream.Stream.of(first, second, third)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
