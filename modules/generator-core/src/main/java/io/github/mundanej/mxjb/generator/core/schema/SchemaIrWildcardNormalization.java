package io.github.mundanej.mxjb.generator.core.schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Package-private wildcard namespace and process-content normalization helpers. */
final class SchemaIrWildcardNormalization {
  private SchemaIrWildcardNormalization() {}

  static String processContents(
      String kindManifestName, String value, Consumer<String> diagnosticSink) {
    if ("skip".equals(value) || "lax".equals(value) || "strict".equals(value)) {
      return value;
    }
    diagnosticSink.accept("xs:" + kindManifestName + " has invalid processContents " + value + ".");
    return null;
  }

  static SchemaIrWildcardNamespace namespace(
      String value, String targetNamespace, Consumer<String> diagnosticSink) {
    String normalized = value == null || value.isBlank() ? "##any" : value.trim();
    List<String> tokens = List.of(normalized.split("\\s+"));
    Set<String> unique = new LinkedHashSet<>(tokens);
    if (unique.size() != tokens.size()) {
      diagnosticSink.accept("xs:any namespace constraint contains duplicate tokens.");
      return null;
    }
    if (tokens.contains("##any")) {
      if (tokens.size() == 1) {
        return new SchemaIrWildcardNamespace("any", List.of());
      }
      diagnosticSink.accept(
          "xs:any namespace ##any cannot be combined with other namespace tokens.");
      return null;
    }
    if (tokens.contains("##other")) {
      if (tokens.size() == 1) {
        return new SchemaIrWildcardNamespace("other", List.of(targetNamespace));
      }
      diagnosticSink.accept(
          "xs:any namespace ##other cannot be combined with other namespace tokens.");
      return null;
    }
    List<String> explicitNamespaces = new ArrayList<>();
    for (String token : tokens) {
      if ("##local".equals(token)) {
        explicitNamespaces.add("");
      } else if ("##targetNamespace".equals(token)) {
        explicitNamespaces.add(targetNamespace);
      } else if (token.startsWith("##")) {
        diagnosticSink.accept("Unsupported xs:any namespace token " + token + ".");
        return null;
      } else {
        explicitNamespaces.add(token);
      }
    }
    return new SchemaIrWildcardNamespace(
        "explicit", new ArrayList<>(new LinkedHashSet<>(explicitNamespaces)));
  }

  static boolean matches(SchemaQName elementName, SchemaIrWildcardNamespace namespace) {
    return switch (namespace.kind()) {
      case "any" -> true;
      case "other" -> !namespace.namespaces().contains(elementName.namespace());
      default -> namespace.namespaces().contains(elementName.namespace());
    };
  }

  static boolean overlap(SchemaIrWildcardNamespace left, SchemaIrWildcardNamespace right) {
    if ("any".equals(left.kind()) || "any".equals(right.kind())) {
      return true;
    }
    if ("other".equals(left.kind()) && "other".equals(right.kind())) {
      return true;
    }
    if ("other".equals(left.kind())) {
      return right.namespaces().stream()
          .anyMatch(namespace -> !left.namespaces().contains(namespace));
    }
    if ("other".equals(right.kind())) {
      return left.namespaces().stream()
          .anyMatch(namespace -> !right.namespaces().contains(namespace));
    }
    return left.namespaces().stream().anyMatch(right.namespaces()::contains);
  }

  static boolean namespaceSubset(
      SchemaIrWildcardNamespace restricted, SchemaIrWildcardNamespace base) {
    if ("any".equals(base.kind())) {
      return true;
    }
    if ("any".equals(restricted.kind())) {
      return "any".equals(base.kind());
    }
    if ("explicit".equals(restricted.kind())) {
      return restricted.namespaces().stream()
          .allMatch(namespace -> matches(new SchemaQName(namespace, "member"), base));
    }
    if ("other".equals(restricted.kind())) {
      return "other".equals(base.kind()) && restricted.namespaces().equals(base.namespaces());
    }
    return false;
  }

  static boolean processContentsAllowsRestriction(String base, String restricted) {
    return processContentsRank(restricted) >= processContentsRank(base);
  }

  static SchemaIrWildcardNamespace union(
      SchemaIrWildcardNamespace left,
      SchemaIrWildcardNamespace right,
      Consumer<String> diagnosticSink) {
    if ("any".equals(left.kind()) || "any".equals(right.kind())) {
      return new SchemaIrWildcardNamespace("any", List.of());
    }
    if (left.kind().equals(right.kind()) && left.namespaces().equals(right.namespaces())) {
      return left;
    }
    if ("explicit".equals(left.kind()) && "explicit".equals(right.kind())) {
      Set<String> namespaces = new LinkedHashSet<>(left.namespaces());
      namespaces.addAll(right.namespaces());
      return new SchemaIrWildcardNamespace("explicit", new ArrayList<>(namespaces));
    }
    diagnosticSink.accept(
        "anyAttribute wildcard namespace composition is unsupported for overlapping ##other constraints.");
    return null;
  }

  static String stricterProcessContents(String left, String right) {
    if ("strict".equals(left) || "strict".equals(right)) {
      return "strict";
    }
    if ("lax".equals(left) || "lax".equals(right)) {
      return "lax";
    }
    return "skip";
  }

  private static int processContentsRank(String value) {
    return switch (value) {
      case "strict" -> 2;
      case "lax" -> 1;
      default -> 0;
    };
  }
}
